workspace "AITT Hardware Interaction" "Modern, clear, and comprehensive architecture for app-to-hardware execution" {

    model {
        engineer = person "Engineer" "Runs diagnostics, connects probes, and executes hardware commands."

        aitt = softwareSystem "AITT" "Desktop application with Java + JNI + native C hardware stack." {
            app = container "Desktop App" "JavaFX UI modules (HardwareController, ScriptingController, app bootstrap)." "Java 20 + JavaFX" {
                tags "App"
            }

            orchestration = container "Core Orchestration" "HardwareService, async execution, connection/log event dispatch." "Java" {
                tags "Core"
            }

            jniGateway = container "JNI Gateway" "HardwareBridge singleton and JNI entry points exposed to Java." "JNI" {
                tags "Bridge"
            }

            interop = container "Java-C Translation" "Type conversion and marshalling across Java/C boundary." "JNI/C Interop" {
                tags "Bridge"
            }

            nativeRuntime = container "Native Runtime" "aitt_native.c: lifecycle, lock, detection, command, memory, registry." "C" {
                tags "Native"
            }

            driverLayer = container "Hardware Abstraction" "Chipset drivers (S32K148 SWD, MPC5777M JTAG)." "C Drivers" {
                tags "Driver"
            }

            transport = container "Physical Transport" "USB probe communication and low-level debug signaling." "USB + SWD/JTAG" {
                tags "Physical"
            }

            target = container "Target Hardware" "Automotive MCU targets: S32K148 / MPC5777M." "Embedded Hardware" {
                tags "Target"
            }
        }

        engineer -> app "Uses"

        app -> orchestration "Scan / connect / command / memory"
        orchestration -> jniGateway "Invoke native operations asynchronously"
        jniGateway -> interop "Marshal and convert data"
        interop -> nativeRuntime "Invoke C APIs"
        nativeRuntime -> driverLayer "Resolve and execute active chipset driver"
        driverLayer -> transport "Run SWD/JTAG transactions"
        transport -> target "Execute on target pins"

        target -> transport "Response/status"
        transport -> driverLayer "Probe feedback"
        driverLayer -> nativeRuntime "Operation result"
        nativeRuntime -> interop "Native output"
        interop -> jniGateway "Converted result"
        jniGateway -> orchestration "Result/error"
        orchestration -> app "Events/logs/callbacks"
    }

    views {
        systemContext aitt "Context" "Who uses AITT and what it controls" {
            include *
            autolayout lr
        }

        container aitt "Architecture" "End-to-end module interconnection from app to physical hardware" {
            include *
            autolayout lr
        }

        dynamic aitt "Discover_Probe_Runtime" "Runtime flow for probe discovery" {
            app -> orchestration "detectHardwareAsync()"
            orchestration -> jniGateway "detectConnectedHardware()"
            jniGateway -> interop "JNI string/array conversion"
            interop -> nativeRuntime "aitt_detect_hardware()"
            nativeRuntime -> transport "Enumerate USB/serial"
            transport -> target "Probe presence check"
            target -> transport "Device metadata"
            transport -> nativeRuntime "Probe list"
            nativeRuntime -> interop "ProbeInfo[]"
            interop -> jniGateway "String[]/objects"
            jniGateway -> orchestration "List<HardwareInfo>"
            orchestration -> app "UI list update callback"
            autolayout lr
        }

        dynamic aitt "Send_Command_Runtime" "Runtime flow for command execution" {
            app -> orchestration "sendCommandAsync(command)"
            orchestration -> jniGateway "HardwareBridge.sendCommand(timeout)"
            jniGateway -> interop "Marshal command"
            interop -> nativeRuntime "aitt_send_command()"
            nativeRuntime -> driverLayer "activeDriver.sendCommand()"
            driverLayer -> transport "SWD/JTAG transfer"
            transport -> target "Execute command"
            target -> transport "Response/status"
            transport -> driverLayer "Raw response"
            driverLayer -> nativeRuntime "CommandResult"
            nativeRuntime -> interop "Native result"
            interop -> jniGateway "CommandResult parse"
            jniGateway -> orchestration "result/error"
            orchestration -> app "event + log callback"
            autolayout lr
        }

        styles {
            element "Element" {
                shape RoundedBox
                background "#0F172A"
                color "#E2E8F0"
                stroke "#334155"
                strokeWidth 2
                fontSize 22
            }

            element "Person" {
                shape Person
                background "#020617"
                color "#F8FAFC"
                stroke "#22D3EE"
            }

            element "Software System" {
                background "#0B1220"
                color "#E2E8F0"
                stroke "#475569"
            }

            element "Container" {
                background "#111827"
                color "#F8FAFC"
                stroke "#4B5563"
            }

            element "App" {
                background "#0F172A"
                stroke "#06B6D4"
            }

            element "Core" {
                background "#111827"
                stroke "#3B82F6"
            }

            element "Bridge" {
                background "#1E1B4B"
                stroke "#A78BFA"
            }

            element "Native" {
                background "#1F2937"
                stroke "#FB7185"
            }

            element "Driver" {
                background "#292524"
                stroke "#F59E0B"
            }

            element "Physical" {
                background "#052E2B"
                stroke "#34D399"
            }

            element "Target" {
                background "#3F0A0A"
                stroke "#F43F5E"
            }

            relationship "Relationship" {
                color "#94A3B8"
                thickness 2
                fontSize 16
                routing Direct
            }
        }

        theme default
    }
}
