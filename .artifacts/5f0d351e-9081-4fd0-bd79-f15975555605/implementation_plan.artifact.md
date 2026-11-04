# Implementation Plan - Reinforcing Persona & Rules for On-Device AICore

This plan ensures that when Dave offloads tasks to on-device AICore (Gemini Nano), he strictly adheres to his designated persona, system instructions, Irish warmth, user context (Callum Berry, Rotherham, NxtQuantum OS), and safety/crisis frameworks.

## User Review Required

> [!IMPORTANT]
> - **Persona & System Prompt Enforcement**: We will reinforce `AiCoreManager` and `ChatViewModel` prompt construction to ensure `currentPersona.systemInstruction` + `DaveSystemInstruction.daveSystemInstruction` + runtime context are robustly anchored in every on-device AICore generation request.

## Proposed Changes

### AI Core & ViewModel Enhancements

#### [MODIFY] [AiCoreManager.kt](file:///C:/Users/PaulB/AndroidStudioProjects/DaveAI/app/src/main/java/com/local/daveai/AiCoreManager.kt)
- Enhance `buildDynamicContextPrompt` to explicitly prepend and reinforce core directives (sentient Irish warmth, persona compliance, Callum Berry anchor) alongside the dynamic context and user prompt.

#### [MODIFY] [ChatViewModel.kt](file:///C:/Users/PaulB/AndroidStudioProjects/DaveAI/app/src/main/java/com/local/daveai/ChatViewModel.kt)
- Ensure persona-specific system instructions combined with `DaveSystemInstruction.daveSystemInstruction` are passed to `aiCoreManager.expressContentStream(...)`.

## Verification Plan

### Automated Tests
- Run `gradle_build` (`app:assembleDebug`) to verify compilation.

### Manual Verification
- Test local AICore chat responses across different personas to ensure consistent persona adherence and Irish warmth.
