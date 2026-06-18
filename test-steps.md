# Notepad Pro - Microsoft Store Testing Notes

This document provides step-by-step instructions and required disclosures for Microsoft Store certification testers. It outlines how to test the application and addresses specific policy requirements.

## 1. General Application Overview

Notepad Pro is a lightweight, local text editor designed for developers and power users, featuring syntax highlighting, robust file management, and an optional integrated AI Assistant.

## 2. Test Accounts and Credentials

**Not Applicable.**
This application does not require user accounts, registration, sign-in, or subscriptions. All core text-editing features are completely free and available immediately upon launch. There are no test accounts required to evaluate this submission.

## 3. Dependencies on non-Microsoft Drivers or NT Services

**None.**
This application is a standalone desktop application. It does not install, bundle, or rely on any non-Microsoft system drivers, kernel modules, or NT services to function.

## 4. Dependencies on Other Products

**None.**
The application is entirely self-contained. It bundles its own necessary runtime environment and does not require the user to separately install Java, Python, Node.js, or any other external frameworks.

## 5. Background Audio Usage

**Not Applicable.**
This application is a text editor. It does not play background audio, nor does it request or utilize background audio permissions.

## 6. Step-by-Step Testing Instructions (Core Features)

Please follow these steps to verify the primary functionality of the application:

1. **Install the App:** Run the provided `.msi` installer. Verify the Publisher displays as "Redix Systems". Progress through the setup, ensuring the "AI Usage Restriction Policy" is visible in the EULA. Choose whether to opt-in to "Send Diagnostic Data" in the feature tree. Finally, verify the "Launch Notepad_Pro" checkbox is available on the finish screen.
2. **Launch the App:** If not auto-launched, open Notepad Pro from the Start Menu.
3. **Create a File:** Click on the "New File" icon in the top toolbar or press `Ctrl+N`. Type some text into the editor pane.
4. **Save the File:** Click the "Save" icon or press `Ctrl+S`. Choose a local directory on your device to save the `.txt` or code file.
5. **File Management & Tabs:** Open a text file to load it in a new tab. Verify the modern, pill-shaped tabs correctly highlight the active document.
6. **Theme Switching:** Click the "Toggle Theme" (sun/moon) icon in the top right header to seamlessly switch between Light Mode and Dark Mode.

## 7. Steps to Access Conditional Content (AI Assistant)

The application includes an AI Chat Assistant feature. This feature is conditional, as it requires the user to supply their own third-party API key to function. We do not provide built-in API keys.

**To test this feature:**

1. Open the application.
2. Click the **"Settings"** (gear icon) or the **"AI"** button in the header.
3. In the settings menu, look for the AI Configuration section and select an AI Provider (e.g., OpenAI, Google Gemini, DeepSeek, or OpenRouter).
4. **Important:** Because this relies on third-party usage quotas, you must enter your own valid API key for the chosen provider into the "API Key" field.
5. Click **Save** to apply the settings.
6. Navigate to the AI Chat panel on the right side of the window. Type a prompt (e.g., _"Hello, write a python script to print hello world"_) and press Enter.
7. Verify that the app successfully communicates with the API provider and streams the response into the chat window.

_(Note to Tester: If you do not have a valid API key to test with, the AI feature will gracefully display an error instructing the user to provide a valid key. This is expected behavior and does not inhibit or block any of the core local text-editing functionalities of the application)._
