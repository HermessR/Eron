package com.aerospace.aitt.remote.service;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.remote.model.NetworkProbe;
import com.aerospace.aitt.remote.model.NetworkProbe.ConnectionStatus;
import com.aerospace.aitt.remote.model.NetworkProbe.RemoteType;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service for discovering and managing network-attached debug probes.
 * 
 * <p>Supports multiple discovery methods:
 * <ul>
 *   <li>UDP broadcast discovery for AITT probe servers</li>
 *   <li>Direct TCP connection to known addresses</li>
 *   <li>Subnet scanning for TRACE32 PowerDebug units</li>
 *   <li>mDNS/DNS-SD discovery (if available)</li>
 * </ul>
 */
public class NetworkProbeService implements AutoCloseable {
    
    private static final AppLogger log = new AppLogger(NetworkProbeService.class);
    
    /** Discovery broadcast port */
    private static final int DISCOVERY_PORT = 20101;
    
    /** Discovery timeout in milliseconds */
    private static final int DISCOVERY_TIMEOUT_MS = 3000;
    
    /** Connection test timeout */
    private static final int CONNECTION_TIMEOUT_MS = 2000;
    
    /** Probe keep-alive interval */
    private static final int KEEPALIVE_INTERVAL_MS = 30000;
    
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final Map<String, NetworkProbe> knownProbes;
    private final Map<String, Socket> activeConnections;
    private final List<Consumer<NetworkProbe>> discoveryListeners;
    private final List<Consumer<String>> logListeners;
    
    private volatile boolean running = true;
    private ScheduledFuture<?> keepAliveTask;
    
    public NetworkProbeService() {
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "network-probe-worker");
            t.setDaemon(true);
            return t;
        });
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "network-probe-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.knownProbes = new ConcurrentHashMap<>();
        this.activeConnections = new ConcurrentHashMap<>();
        this.discoveryListeners = new CopyOnWriteArrayList<>();
        this.logListeners = new CopyOnWriteArrayList<>();
        
        log.info("NetworkProbeService initialized");
    }
    
    // ==================== LISTENERS ====================
    
    /**
     * Adds a listener for probe discovery events.
     */
    public void addDiscoveryListener(Consumer<NetworkProbe> listener) {
        discoveryListeners.add(listener);
    }
    
    /**
     * Removes a discovery listener.
     */
    public void removeDiscoveryListener(Consumer<NetworkProbe> listener) {
        discoveryListeners.remove(listener);
    }
    
    /**
     * Adds a log listener.
     */
    public void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }
    
    private void fireDiscovery(NetworkProbe probe) {
        discoveryListeners.forEach(l -> {
            try {
                l.accept(probe);
            } catch (Exception e) {
                log.error("Discovery listener error", e);
            }
        });
    }
    
    private void fireLog(String message) {
        log.debug(message);
        logListeners.forEach(l -> {
            try {
                l.accept(message);
            } catch (Exception ignored) {}
        });
    }
    
    // ==================== DISCOVERY ====================
    
    /**
     * Discovers network probes using all available methods.
     * 
     * @return CompletableFuture with list of discovered probes
     */
    public CompletableFuture<List<NetworkProbe>> discoverAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<NetworkProbe> discovered = new ArrayList<>();
            
            fireLog("Starting network probe discovery...");
            
            // Run discovery methods in parallel
            List<CompletableFuture<List<NetworkProbe>>> futures = new ArrayList<>();
            
            // UDP broadcast discovery
            futures.add(discoverViaBroadcast());
            
            // Scan common ports on local subnet
            futures.add(scanLocalSubnet());
            
            // Wait for all methods to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            for (CompletableFuture<List<NetworkProbe>> future : futures) {
                try {
                    discovered.addAll(future.get());
                } catch (Exception e) {
                    log.warn("Discovery method failed", e);
                }
            }
            
            // Deduplicate and update known probes
            Map<String, NetworkProbe> unique = new HashMap<>();
            for (NetworkProbe probe : discovered) {
                unique.putIfAbsent(probe.id(), probe);
                knownProbes.put(probe.id(), probe);
                fireDiscovery(probe);
            }
            
            fireLog("Discovery complete: found " + unique.size() + " probe(s)");
            return new ArrayList<>(unique.values());
            
        }, executor);
    }
    
    /**
     * Discovers probes via UDP broadcast.
     */
    private CompletableFuture<List<NetworkProbe>> discoverViaBroadcast() {
        return CompletableFuture.supplyAsync(() -> {
            List<NetworkProbe> probes = new ArrayList<>();
            
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout(DISCOVERY_TIMEOUT_MS);
                
                // Send discovery packet
                byte[] request = "AITT_PROBE_DISCOVER".getBytes(StandardCharsets.UTF_8);
                InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(request, request.length, broadcast, DISCOVERY_PORT);
                socket.send(packet);
                
                fireLog("Sent broadcast discovery to port " + DISCOVERY_PORT);
                
                // Receive responses
                byte[] buffer = new byte[1024];
                long deadline = System.currentTimeMillis() + DISCOVERY_TIMEOUT_MS;
                
                while (System.currentTimeMillis() < deadline) {
                    try {
                        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                        socket.receive(response);
                        
                        String data = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
                        NetworkProbe probe = parseDiscoveryResponse(data, response.getAddress());
                        if (probe != null) {
                            probes.add(probe);
                            fireLog("Discovered via broadcast: " + probe.address());
                        }
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }
                
            } catch (Exception e) {
                log.debug("Broadcast discovery failed: %s", e.getMessage());
            }
            
            return probes;
        }, executor);
    }
    
    /**
     * Scans local subnet for TRACE32 probes.
     */
    private CompletableFuture<List<NetworkProbe>> scanLocalSubnet() {
        return CompletableFuture.supplyAsync(() -> {
            List<NetworkProbe> probes = new ArrayList<>();
            
            try {
                // Get local IP to determine subnet
                String localIp = getLocalIpAddress();
                if (localIp == null) {
                    return probes;
                }
                
                String subnet = localIp.substring(0, localIp.lastIndexOf('.') + 1);
                fireLog("Scanning subnet: " + subnet + "0/24");
                
                // Scan common host range concurrently
                List<CompletableFuture<NetworkProbe>> scanFutures = new ArrayList<>();
                
                for (int i = 1; i < 255; i++) {
                    String host = subnet + i;
                    scanFutures.add(probeHost(host, NetworkProbe.DEFAULT_T32_PORT, RemoteType.T32_REMOTE_API));
                    scanFutures.add(probeHost(host, NetworkProbe.DEFAULT_AITT_PORT, RemoteType.AITT_PROBE_SERVER));
                }
                
                // Wait with timeout
                try {
                    CompletableFuture.allOf(scanFutures.toArray(new CompletableFuture[0]))
                        .get(DISCOVERY_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    // Expected for slow networks
                }
                
                for (CompletableFuture<NetworkProbe> future : scanFutures) {
                    try {
                        NetworkProbe probe = future.getNow(null);
                        if (probe != null) {
                            probes.add(probe);
                        }
                    } catch (Exception ignored) {}
                }
                
            } catch (Exception e) {
                log.debug("Subnet scan failed: %s", e.getMessage());
            }
            
            return probes;
        }, executor);
    }
    
    /**
     * Probes a specific host:port for a debug service.
     */
    private CompletableFuture<NetworkProbe> probeHost(String host, int port, RemoteType expectedType) {
        return CompletableFuture.supplyAsync(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
                socket.setSoTimeout(CONNECTION_TIMEOUT_MS);
                
                // Try to identify the service
                RemoteType detectedType = identifyService(socket, expectedType);
                if (detectedType != RemoteType.UNKNOWN) {
                    NetworkProbe probe = NetworkProbe.discovered(host, port, detectedType);
                    fireLog("Found probe at " + host + ":" + port);
                    return probe;
                }
            } catch (Exception ignored) {
                // Host not responding
            }
            return null;
        }, executor);
    }
    
    /**
     * Attempts to identify the service type on a socket.
     */
    private RemoteType identifyService(Socket socket, RemoteType expected) {
        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            // Send identification request based on expected type
            if (expected == RemoteType.AITT_PROBE_SERVER) {
                out.write("AITT_PING\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                
                byte[] response = new byte[256];
                int read = in.read(response);
                if (read > 0) {
                    String resp = new String(response, 0, read, StandardCharsets.UTF_8);
                    if (resp.startsWith("AITT_PONG")) {
                        return RemoteType.AITT_PROBE_SERVER;
                    }
                }
            } else if (expected == RemoteType.T32_REMOTE_API) {
                // TRACE32 Remote API uses a specific handshake
                // For now, just check if port is open
                return RemoteType.T32_REMOTE_API;
            }
            
        } catch (Exception e) {
            log.debug("Service identification failed: %s", e.getMessage());
            return RemoteType.UNKNOWN; // Return UNKNOWN on error, not assumed type
        }
        
        return expected; // Assume expected type if port is open and no error
    }
    
    /**
     * Parses a discovery response from UDP broadcast.
     */
    private NetworkProbe parseDiscoveryResponse(String data, InetAddress sender) {
        // Expected format: "AITT_PROBE:port:name:type:firmware:serial"
        try {
            if (data.startsWith("AITT_PROBE:")) {
                String[] parts = data.substring(11).split(":");
                if (parts.length >= 2) {
                    int port = Integer.parseInt(parts[0]);
                    String name = parts.length > 1 ? parts[1] : "Remote Probe";
                    RemoteType type = RemoteType.AITT_PROBE_SERVER;
                    String firmware = parts.length > 3 ? parts[3] : null;
                    String serial = parts.length > 4 ? parts[4] : null;
                    
                    NetworkProbe probe = NetworkProbe.discovered(sender.getHostAddress(), port, type);
                    if (firmware != null) {
                        probe = probe.withFirmware(firmware, serial);
                    }
                    return probe;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse discovery response: %s", data);
        }
        return null;
    }
    
    // ==================== MANUAL CONNECTION ====================
    
    /**
     * Adds a probe manually by host and port.
     * 
     * @param name Display name for the probe
     * @param host IP address or hostname
     * @param port TCP port
     * @param type Expected remote type
     * @return CompletableFuture with the probe if connection succeeds
     */
    public CompletableFuture<NetworkProbe> addManualProbe(String name, String host, int port, RemoteType type) {
        return CompletableFuture.supplyAsync(() -> {
            fireLog("Testing connection to " + host + ":" + port);
            
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
                
                NetworkProbe probe = NetworkProbe.manual(name, host, port, type);
                knownProbes.put(probe.id(), probe);
                fireDiscovery(probe);
                
                fireLog("Successfully added probe: " + probe.displayString());
                return probe;
                
            } catch (Exception e) {
                fireLog("Failed to connect to " + host + ":" + port + " - " + e.getMessage());
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    // ==================== CONNECTION MANAGEMENT ====================
    
    /**
     * Connects to a network probe.
     */
    public CompletableFuture<Boolean> connect(NetworkProbe probe) {
        return CompletableFuture.supplyAsync(() -> {
            fireLog("Connecting to " + probe.address());
            
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(probe.host(), probe.port()), CONNECTION_TIMEOUT_MS);
                socket.setKeepAlive(true);
                socket.setSoTimeout(30000);
                
                activeConnections.put(probe.id(), socket);
                
                NetworkProbe connected = probe.withStatus(ConnectionStatus.CONNECTED);
                knownProbes.put(probe.id(), connected);
                fireDiscovery(connected);
                
                fireLog("Connected to " + probe.name());
                return true;
                
            } catch (Exception e) {
                fireLog("Connection failed: " + e.getMessage());
                
                NetworkProbe failed = probe.withStatus(ConnectionStatus.ERROR);
                knownProbes.put(probe.id(), failed);
                fireDiscovery(failed);
                
                return false;
            }
        }, executor);
    }
    
    /**
     * Disconnects from a network probe.
     */
    public void disconnect(NetworkProbe probe) {
        Socket socket = activeConnections.remove(probe.id());
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
        
        NetworkProbe disconnected = probe.withStatus(ConnectionStatus.DISCONNECTED);
        knownProbes.put(probe.id(), disconnected);
        fireDiscovery(disconnected);
        fireLog("Disconnected from " + probe.name());
    }
    
    /**
     * Gets the socket for a connected probe.
     */
    public Optional<Socket> getConnection(NetworkProbe probe) {
        return Optional.ofNullable(activeConnections.get(probe.id()));
    }
    
    /**
     * Checks if a probe is connected.
     */
    public boolean isConnected(NetworkProbe probe) {
        Socket socket = activeConnections.get(probe.id());
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    // ==================== PROBE COMMANDS ====================
    
    /**
     * Sends a command to a connected probe and returns the response.
     */
    public CompletableFuture<String> sendCommand(NetworkProbe probe, String command) {
        return CompletableFuture.supplyAsync(() -> {
            Socket socket = activeConnections.get(probe.id());
            if (socket == null || socket.isClosed()) {
                throw new CompletionException(new IOException("Not connected to probe"));
            }
            
            try {
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                
                // Send command
                out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
                
                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                
                // Read until empty line or timeout
                socket.setSoTimeout(5000);
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) break;
                    response.append(line).append("\n");
                }
                
                return response.toString().trim();
                
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    // ==================== KEEP-ALIVE ====================
    
    /**
     * Starts periodic keep-alive checks for connected probes.
     */
    public void startKeepAlive() {
        if (keepAliveTask != null) {
            keepAliveTask.cancel(false);
        }
        
        keepAliveTask = scheduler.scheduleAtFixedRate(() -> {
            // Collect keys to remove to avoid concurrent modification
            List<String> keysToRemove = new ArrayList<>();
            
            for (Map.Entry<String, Socket> entry : activeConnections.entrySet()) {
                Socket socket = entry.getValue();
                if (socket.isClosed()) {
                    NetworkProbe probe = knownProbes.get(entry.getKey());
                    if (probe != null) {
                        NetworkProbe disconnected = probe.withStatus(ConnectionStatus.DISCONNECTED);
                        knownProbes.put(entry.getKey(), disconnected);
                        fireDiscovery(disconnected);
                    }
                    keysToRemove.add(entry.getKey());
                }
            }
            
            // Remove closed connections outside of iteration
            for (String key : keysToRemove) {
                activeConnections.remove(key);
            }
        }, KEEPALIVE_INTERVAL_MS, KEEPALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Stops keep-alive checks.
     */
    public void stopKeepAlive() {
        if (keepAliveTask != null) {
            keepAliveTask.cancel(false);
            keepAliveTask = null;
        }
    }
    
    // ==================== UTILITIES ====================
    
    /**
     * Gets the list of known probes.
     */
    public List<NetworkProbe> getKnownProbes() {
        return new ArrayList<>(knownProbes.values());
    }
    
    /**
     * Gets the list of connected probes.
     */
    public List<NetworkProbe> getConnectedProbes() {
        return knownProbes.values().stream()
            .filter(p -> p.status() == ConnectionStatus.CONNECTED)
            .toList();
    }
    
    /**
     * Gets the local IP address.
     */
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get local IP", e);
        }
        return null;
    }
    
    @Override
    public void close() {
        running = false;
        stopKeepAlive();
        
        // Close all connections
        for (Socket socket : activeConnections.values()) {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
        activeConnections.clear();
        
        executor.shutdown();
        scheduler.shutdown();
        
        log.info("NetworkProbeService closed");
    }
}
