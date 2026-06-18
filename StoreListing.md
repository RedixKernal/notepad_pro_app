# Application Description

**Notepad Pro** is a modern, lightweight, and incredibly powerful text editor designed to maximize your productivity. Whether you're jotting down quick ideas, managing complex code projects across multiple tabs, or utilizing intelligent AI assistance, Notepad Pro provides a seamless and elegant environment for all your text editing needs. 

**Key Features:**
*   **AI Assistant:** Instantly analyze, summarize, or generate code right from your active tab.
*   **Multi-Tab Editing:** Effortlessly juggle multiple documents simultaneously.
*   **Seamless File Management:** Easily create, open, update, and delete files directly within the editor.
*   **Universal Syntax Support:** Compile and execute over 50 programming languages.
*   **Command Palette:** Lightning-fast access to files and tools using keyboard shortcuts.
*   **Auto-Save & Deep Search:** Never lose your progress, and find exactly what you need instantly.

# What's New

**Version 2.0.0**
*   **[New] AI Assistant Integration:** Interact with advanced AI models directly within your editor. Use the context of your current file to ask questions, debug code, or generate text seamlessly.
*   **[New] Modern UI Refinements:** Enjoy beautiful, realistic pill-shaped tabs with distinct active and inactive states for a smooth, Chrome/Windows 11-inspired user experience.
*   **[New] Enhanced Installer:** A seamless MSI installer featuring publisher verification (Redix Systems), a built-in AI Usage Restriction Policy EULA, auto-launch support, and an optional anonymous diagnostic reporting feature.
*   **[Improved] Performance Boost:** Enhanced window responsiveness and improved memory management for large files.
*   **[Fixed] UI/UX Refinements:** Addressed minor rendering glitches in Dark Mode, optimized left and top layout spacing, and improved dialog box placements for a smoother experience.

---

*© 2026 RedixKernal. All rights reserved. Notepad Pro™ is a trademark of RedixKernal.*

# Additional License Terms

By acquiring and using Notepad Pro, you agree to the Standard Application License Terms with the following additions/amendments:

1. **"AS IS" BASIS:** The software is provided "as is", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and non-infringement.
2. **LIMITATION OF LIABILITY:** In no event shall the authors or copyright holders be liable for any claim, damages, or other liability, whether in an action of contract, tort or otherwise, arising from, out of, or in connection with the software or the use or other dealings in the software.
3. **REVERSE ENGINEERING:** You may not reverse engineer, decompile, or disassemble the software, except and only to the extent that such activity is expressly permitted by applicable law notwithstanding this limitation.

# Restricted Capabilities Justification

**Capability:** `runFullTrust`

**Why do you need the runFullTrust capability, and how will it be used in your product?**

Notepad Pro is a traditional desktop application that has been packaged for distribution through the Microsoft Store. As a comprehensive code editor and development environment, the application requires the `runFullTrust` capability to perform its core functions. 

Specifically, this capability is used for:
1.  **Unrestricted File System Access:** To allow users to directly open, edit, save, create, and manage files from arbitrary locations on their hard drive without being hindered by the AppContainer sandbox file picker restrictions.
2.  **Process Execution:** To support the "Universal Syntax Support" feature. The application needs to break out of the sandbox to spawn external processes and invoke local toolchains (such as compilers, interpreters, and debuggers like Python, Node.js, or GCC) installed on the user's system to compile and execute user code. 

Without `runFullTrust`, Notepad Pro's core file management and code execution features would be rendered entirely non-functional.
