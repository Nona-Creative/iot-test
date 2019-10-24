# IoT Credentials Reconnection Example

All the relevant logic is in IoTManager.kt. Required configuration parameters can be found at the bottom of the file.

To trigger the bug, start the app, then disable internet connectivity for the app or device to trigger the keepalive timeout and reconnection attempts. When the credentials expire (after about an hour), you will see the issue.
