<p align="center" style="font-size: 48px; font-weight: 800; margin: 0;">OBDManager</p>

<p align="center">
  <img width="368" height="360" alt="Captura de pantalla 2025-05-13 214237" src="https://github.com/user-attachments/assets/33ead279-c923-4740-ac7a-f753285aae86" />
</p>

🚗 Smart Vehicle Maintenance Assistant
This Android application is the result of my Final Degree Project (TFG). It connects to a vehicle's ECU using an OBD-II USB adapter and uses artificial intelligence to provide personalized maintenance recommendations based on real-time diagnostic data.

🛠️ Features
Connects to the car via OBD-II using a USB adapter

Reads real-time data and trouble codes (DTCs) from the ECU

Uses Google's Gemini AI to analyze data and suggest maintenance actions

Interactive chat interface to ask questions and receive explanations

Local database with vehicle models, diagnostic parameters, and error codes

Built with Kotlin and Jetpack Compose

🤖 How It Works
The app connects to the vehicle's OBD port and retrieves data such as temperatures, pressures, and diagnostic trouble codes (DTCs).

It sends this information to Gemini AI, which analyzes the data and generates customized maintenance suggestions (e.g., oil change, coolant refill, tire pressure check).

The user can interact with the integrated chat interface to ask questions and better understand the vehicle's condition.

🎯 Goal
To make vehicle maintenance smarter and more accessible by combining real-time OBD-II diagnostics with artificial intelligence.
