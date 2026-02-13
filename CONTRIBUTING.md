Contributing to PowerHUD

This project is developed using a modern, AI-augmented workflow. I am not a professional coder; I act as the architect and lead developer using Gemini for logic generation, VS Code for environment management, and PowerShell/Gradle for automated deployment.
🤖 AI-First Workflow

    Code Generation: If you are suggesting changes, please provide the entire file rather than snippets. This allows me to easily swap files within VS Code and run my automation scripts.

    Preservation Rule: Established features (Liquid/Entity Detection, Ghost Air, Day/Night math, and Dynamic FPS) are immutable baselines. Preserve them verbatim unless a specific change is requested.

    No Redesigns: The configuration UI is locked. Focus on functional data and performance logic.

⚙️ Environment & Build

    Tooling: We use Gradle for dependency management. Ensure all changes are compatible with .\gradlew.bat.

    Deployment: All code must pass my Unified Execution & Deployment Script, which clears the screen, updates version metadata in gradle.properties and fabric.mod.json, builds, runs the client, and manages the /Archive and /Test folders.

    GSON Safety: Always use Static Inner Classes for layout entries to prevent circular reference crashes during GSON serialization.

💬 Communication

    The "Steve" Identity: You can address me as Steve.

    Functional Explanations: Keep non-technical talk high-level and short.

    Agreement First: If you have a question or a feature idea, let's agree on the design via conversation first. Do not provide code until we have reached an agreement.
