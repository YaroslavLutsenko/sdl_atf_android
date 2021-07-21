# SDL ATF Android

This is a mobile application according to the new version of [proposal (0126)](https://github.com/LuxoftSDL/sdl_evolution/pull/11). Doesn’t use sdl_android lib.

## Projcet structure

* Http module.
Perform control communication with ATF according REST API and transfer commands to core module.
* ATF module
Responsible for connections and communications to ATF via TCP
* Transport module
Responsible for connections and communications to SDL via available transports.
* Core module
Main module of the application. Execute commands from ATF and conducts transferring data between ATF and SDL.
* View module
Displays information on device.

###### The state of progress:

- [X] Http module (except GetDeviceInfo data)
- [ ] Core module
- [ ] View module
- [ ] Transport module
- [X] ATF module
- [ ] Additional logic
