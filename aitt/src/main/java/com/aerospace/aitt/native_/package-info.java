/**
 * Native JNI bridge module for hardware communication.
 * 
 * <p>This package contains Java classes that declare native methods
 * implemented in C/C++ for low-level hardware access including:
 * <ul>
 *   <li>USB debug probe detection and communication</li>
 *   <li>Serial port enumeration and access</li>
 *   <li>CMM command execution via TRACE32 API</li>
 *   <li>Chipset-specific protocol handlers</li>
 * </ul>
 * 
 * <h2>Supported Chipsets</h2>
 * <ul>
 *   <li>NXP S32K148 - ARM Cortex-M4F automotive MCU</li>
 *   <li>NXP MPC5777M - Power Architecture automotive MCU</li>
 * </ul>
 * 
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    JavaFX UI Layer                         │
 * ├─────────────────────────────────────────────────────────────┤
 * │                 Java Application Logic                      │
 * │  (FlashingService, ConfigService, TestDevService)          │
 * ├─────────────────────────────────────────────────────────────┤
 * │              Native Bridge (this package)                   │
 * │  HardwareBridge.java ─► JNI ─► aitt_native.dll             │
 * ├─────────────────────────────────────────────────────────────┤
 * │                    C/C++ Native Layer                       │
 * │  (USB, Serial, Debug Probe, Chipset Protocols)             │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * @since 1.0
 */
package com.aerospace.aitt.native_;
