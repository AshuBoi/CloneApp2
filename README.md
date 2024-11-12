
# Remote VNC Connect Application - Setup Guide

This guide will help you set up the Remote VNC Connect Application. You'll learn how to add JavaFX to IntelliJ, run the broker server, and connect two systems using the application.

## Prerequisites

- **Java Development Kit (JDK)**: Ensure JDK 11 or later is installed.
- **IntelliJ IDEA**: A popular Java IDE to run the Java application.
- **Python**: Python 3.x for the broker server.
- **Node.js (Optional)**: If you prefer to run the broker server using JavaScript.
- **Internet Access**: To enable communication between the host and client systems.

## Step 1: Setting Up JavaFX in IntelliJ

### 1.1 Install JavaFX SDK

1. Download JavaFX SDK from [https://gluonhq.com/products/javafx/](https://gluonhq.com/products/javafx/).
2. Extract the downloaded SDK to a suitable location on your system.

### 1.2 Configure JavaFX in IntelliJ

1. **Open IntelliJ IDEA** and create or open your project.
2. Go to **File > Project Structure**.
3. Under **Modules**, click on **Dependencies**.
4. Click the **+** icon to add a new library, and select **Java**.
5. Navigate to the `lib` folder inside the extracted JavaFX SDK and add it.

### 1.3 Update VM Options

1. Go to **Run > Edit Configurations**.
2. In the **VM options** field, add the following:
   ```
   --module-path "<path-to-javafx-lib>" --add-modules javafx.controls,javafx.fxml
   ```
   Replace `<path-to-javafx-lib>` with the path to the `lib` folder of your JavaFX SDK.

## Step 2: Running the Broker Server

The broker server facilitates communication between the host and client systems. You can implement it using Python or Node.js. Below, we'll provide instructions for the Python version.

### 2.1 Python Broker Server Setup

#### 2.1.1 Install Python Dependencies

1. Install Python 3.x if not already installed.

2. Install Flask to create the broker server:
   ```
   pip install flask
   ```

3. Install CORS support for Flask:
   ```
   pip install flask-cors
   ```

#### 2.1.2 Run the Broker Server

1. Save the following script as `broker_server.py`:
   ```python
   from flask import Flask, request, jsonify
   from flask_cors import CORS
   import uuid

   app = Flask(__name__)
   CORS(app)  # Enable Cross-Origin Resource Sharing (CORS)

   # In-memory storage for sessions
   sessions = {}

   @app.route('/registerHost', methods=['POST'])
   def register_host():
       data = request.get_json()
       if not data or 'port' not in data:
           return jsonify({'error': 'Port is required'}), 400

       session_code = str(uuid.uuid4())
       port = data['port']
       ip = request.remote_addr

       # Save the session code with the host details (IP and port)
       sessions[session_code] = {'ip': ip, 'port': port}

       return jsonify({'sessionCode': session_code})

   @app.route('/connectClient', methods=['GET'])
   def connect_client():
       session_code = request.args.get('sessionCode')

       if not session_code or session_code not in sessions:
           return jsonify({'error': 'Session code not found'}), 404

       host_details = sessions[session_code]
       return jsonify({'ip': host_details['ip'], 'port': host_details['port']})

   if __name__ == '__main__':
       app.run(host='0.0.0.0', port=3000)
   ```

2. Run the broker server:
   ```
   python broker_server.py
   ```

3. The broker server will start at `http://localhost:3000`.

## Step 3: Running the Remote VNC Connect Application and Connecting Two Laptops for Screen Sharing

### 3.1 Running the Application

To run the Remote VNC Connect Application, you need two laptops:

- **Laptop A (Host)**: This will host the session.
- **Laptop B (Client)**: This will connect to the hosted session.

Both laptops should be connected to the same network. Connecting via Ethernet will provide a more stable and faster connection compared to Wi-Fi, which is especially useful for screen sharing and remote control.

If both laptops are on the same Wi-Fi or Ethernet network, they can communicate directly through the broker server without any additional configuration or special network settings.

For **Ethernet Connection**:
- Connect both laptops to the same router using Ethernet cables.
- This will reduce latency and provide a more reliable connection compared to Wi-Fi.

### 3.2 Running the Java Application

### 3.3 Running the Java Application

1. **Open the project in IntelliJ IDEA**.
2. **Run the ********************`CloneApp`******************** Java class**.

### 3.4 Hosting a Session

1. Click on **Host a Session**.
2. The application will attempt to register with the broker server.
3. A **session code** will be generated. Share this code with the client who wants to connect.

### 3.5 Joining a Session

1. On the client side, run the **Remote VNC Connect Application**.
2. Click on **Join a Session**.
3. Enter the **session code** provided by the host.
4. The application will connect to the broker server, retrieve the host's details, and establish the connection.

## Screen Sharing and Accessibility

### 3.6 Screen Sharing Details

The Remote VNC Connect Application allows the **host's screen** to be shared with the client in real time. The client can see the host's screen and interact using keyboard and mouse inputs. The following features are accessible:

- **Screen View**: The client can see the entire desktop of the host.
- **Mouse Control**: The client can move the mouse, click, and perform drag actions on the host's system.
- **Keyboard Input**: The client can type and control the host's system using their keyboard.

### Limitations
- **Performance**: The screen-sharing quality and responsiveness depend on the network speed. Using an Ethernet connection will provide a smoother experience compared to Wi-Fi.
- **Full Desktop Access**: The entire desktop is accessible, which means the client has full visibility and control. Be cautious when sharing sensitive information.

## Restrictions and Network Requirements

- **Same Network Connection**: If the host and client are connected via the same Wi-Fi or Ethernet network, no additional setup is needed. The broker server will be accessible as long as both devices are on the same local network.

- **Host and Client Network Requirements**: The host and client applications must be able to reach the broker server over the network. Both systems can either be on the same local network (e.g., home or office network) or use public IP addresses with proper port forwarding if over the internet.

- **Broker Server Accessibility**: The broker server must be accessible to both the host and client. Ensure there are no firewalls or network restrictions preventing access to the broker server on port 3000. If access issues occur, follow these steps:

1. **Check Firewall Settings**:
   - On both the host and client systems, ensure that port 3000 is allowed through the firewall.
   - For Windows, go to **Control Panel > System and Security > Windows Defender Firewall > Advanced settings** and create an inbound rule to allow port 3000.
   - On Linux, use `ufw` to allow the port: `sudo ufw allow 3000`.

2. **Router Configuration**:
   - If using a router, make sure that port 3000 is not blocked and is properly forwarded if needed. The broker server runs on `http://<broker-server-ip>:3000`, so ensure that this address is accessible.

3. **Network Restrictions**:
   - Ensure that the network (e.g., office network) does not have restrictions blocking port 3000. You may need to contact your network administrator. The broker server address is `http://<broker-server-ip>:3000`, and it should be reachable from both the host and client.

4. **Testing Connectivity**:
   - Test the broker server by accessing `http://<broker-server-ip>:3000` from both the host and client systems in a web browser to ensure it's reachable.

- **Firewall and Security**: If either the host or client is behind a firewall, ensure that the required ports are open to allow communication.

## Troubleshooting

- **JavaFX Errors**: Ensure the JavaFX SDK is correctly added and that the VM options are set properly.
- **Broker Server Issues**: If the broker server is not reachable, ensure it is running and accessible from both the host and client systems.
- **Network Issues**: Make sure both the host and client systems are on the same network or have proper port forwarding if connecting over the internet.

## Summary

This guide walks you through setting up the Remote VNC Connect Application, configuring JavaFX in IntelliJ, running the broker server, and using it to facilitate easy connections between host and client systems. If you have any questions or run into issues, please reach out for further assistance.
