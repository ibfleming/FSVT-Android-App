<img src="https://github.com/ibfleming/fsvt-app/blob/main/usda_logo.png" alt="logo" width="100"/>

# Free Stream Velocity Team - Android Application

**Version:** 4.0 (LTS)

**Author:** Ian Fleming

**Senior Capstone Project - University of Idaho 2023**

Welcome to the repository containing the software for our Android application for our Senior Design Capstone Project at the University of Idaho in 2023. This project, led by the Free Stream Velocity Team, aimed to develop a responsive, flexible, and modern application that interfaces with our microcontrollers that visualizes and exports data.

## Table of Contents
- [Overview](#overview)
  - [Features](#features)
  - [User Interface Snippet](#user-interface-snippet)
- [Usage](#usage)
  - [Cloning the Repository](#cloning-the-repository)
  - [Installing the Application](#installing-the-application)
- [License](#license)

## Overview

Our project leverages Kotlin language and modern AndroidX implementations in Android Studio to develop a sophisticated Android application. With compatibility across newer and older Android versions, our application seamlessly integrates Bluetooth support to communicate with microcontrollers, enabling real-time data feedback and user command input. Key highlights include stringent permission management, a sleek user interface, dynamic data visualization, and automatic data export in CSV format to the device's local storage.

### Features
- **Bluetooth Integration**: Establishes robust communication with microcontrollers, enabling real-time data exchange and user interaction.
- **Cross-Version Compatibility**: Ensures a seamless experience across various Android versions, prioritizing accessibility and performance.
- **Permission Management**: Implements stringent permission checks to guarantee smooth operation and user security.
- **Sleek User Interface**: Boasts an intuitive and visually appealing interface for enhanced user experience.
- **Live Data Visualization**: Provides dynamic visualization of data, empowering users with real-time insights.
- **Automatic Data Export**: Facilitates effortless data management by automatically exporting data in CSV format to the device's local storage.

### User Interface Snippet
<img src="https://github.com/ibfleming/fsvt-app/blob/main/app_image.png" alt="gui_example" width="300"/>

## Usage

### Cloning the Repository

To clone the repository to your local environment, use the following command:

```bash
git clone https://github.com/ibfleming/fsvt-nano.git
```

### Installing the Application

To easily install the application on your Android device, follow these simple steps:
1. Download the `FSVTApp.apk` from the `apk/` folder in this repository.
2. Transfer the downloaded file to your Android device.
3. Enable installation from unknown sources on your device by navigating to Settings > Security > Unknown sources (or Settings > Apps > Special access > Install unknown apps, depending on your device).
4. Locate the transferred `FSVTApp.apk` file on your Android device and tap on it to begin the installation process.
5. Follow the on-screen instructions to complete the installation.

By following these steps, you'll be able to manually install the application and start using it on your Android device.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
