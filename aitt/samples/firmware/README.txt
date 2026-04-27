AITT Sample Firmware Files for Testing
======================================

These are sample binary files for testing the Flashing Module.
They contain random data and are NOT actual firmware.

Files:
------
bootloader_v1.2.3.bin       - Sample bootloader (16 KB)
                              Suggested flash address: 0x08000000

app_flight_control_v2.0.1.bin - Flight control application (64 KB)
                                Suggested flash address: 0x08010000

sensor_driver_imu_v1.0.0.bin  - IMU sensor driver (8 KB)
                                Suggested flash address: 0x08020000

nav_module_gps_v3.1.4.bin     - GPS navigation module (32 KB)
                                Suggested flash address: 0x08030000

comm_stack_arinc429_v1.5.2.bin - ARINC 429 communication stack (24 KB)
                                 Suggested flash address: 0x08040000

Testing Instructions:
--------------------
1. Login to AITT with any license key and credentials (password 4+ chars)
2. Go to Dashboard > Flashing Module
3. Select a target profile (e.g., Cortex-R5 MCU)
4. Click "Add Module" and select one or more .bin files
5. Enter the suggested flash addresses
6. Click "Validate" to verify addresses
7. Set output path for CMM script (e.g., C:\temp\flash_script.cmm)
8. Click "Generate CMM" to create the TRACE32 script

Note: These files are for UI testing only. No actual hardware flashing will occur
unless TRACE32 is installed and connected to a debug probe.
