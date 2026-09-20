````javascript
/* =========================================================
   HACKER ANKIT AI
   FRONTEND APPLICATION
   CHAT + IMAGE GENERATION
   STORAGE-SAFE VERSION
========================================================= */

"use strict";

/* =========================================================
   CONFIGURATION
========================================================= */

const API_BASE_URL = "";

/*
 * Storage limits
 * IMPORTANT:
 * Base64 images are NEVER saved into localStorage.
 */
const MAX_HISTORY_CHATS = 10;
const MAX_MESSAGES_PER_CHAT = 30;
const MAX_HISTORY_TEXT_LENGTH = 6000;


/* =========================================================
   ELEMENTS
========================================================= */

const loginScreen = document.getElementById("loginScreen");
const appScreen = document.getElementById("appScreen");

const nameInput = document.getElementById("nameInput");
const loginBtn = document.getElementById("loginBtn");
const loginError = document.getElementById("loginError");

const welcomeTitle = document.getElementById("welcomeTitle");
const userNameDisplay = document.getElementById("userNameDisplay");
const userAvatar = document.getElementById("userAvatar");

const messages = document.getElementById("messages");
const welcomeScreen = document.getElementById("welcomeScreen");

const messageInput = document.getElementById("messageInput");
const sendBtn = document.getElementById("sendBtn");
const micBtn = document.getElementById("micBtn");

const imageInput = document.getElementById("imageInput");
const imagePreviewBox = document.getElementById("imagePreviewBox");
const imagePreview = document.getElementById("imagePreview");
const removeImageBtn = document.getElementById("removeImageBtn");

const typingIndicator = document.getElementById("typingIndicator");

const voiceToggleBtn = document.getElementById("voiceToggleBtn");

const newChatBtn = document.getElementById("newChatBtn");
const sidebarNewChatBtn = document.getElementById("sidebarNewChatBtn");

const historyList = document.getElementById("historyList");

const logoutBtn = document.getElementById("logoutBtn");

const menuBtn = document.getElementById("menuBtn");
const sidebar = document.getElementById("sidebar");
const sidebarOverlay = document.getElementById("sidebarOverlay");

const quickPrompts = document.querySelectorAll(".quick-prompt");
const toolButtons = document.querySelectorAll(".tool-btn");

const imageModal = document.getElementById("imageModal");
const closeImageModal = document.getElementById("closeImageModal");
const imagePromptInput = document.getElementById("imagePromptInput");
const generateImageBtn = document.getElementById("generateImageBtn");
const imageGenerationStatus =
    document.getElementById("imageGenerationStatus");

const generatedImageContainer =
    document.getElementById("generatedImageContainer");

const generatedImage =
    document.getElementById("generatedImage");


/* =========================================================
   SAFE STORAGE HELPERS
========================================================= */

function safeGet(key, fallback = "") {

    try {
        const value = localStorage.getItem(key);

        return value === null
            ? fallback
            : value;

    } catch (error) {

        console.warn(
            "⚠️ localStorage read failed:",
            error
        );

        return fallback;
    }
}


function safeSet(key, value) {

    try {

        localStorage.setItem(
            key,
            value
        );

        return true;

    } catch (error) {

        console.warn(
            "⚠️ localStorage write failed:",
            error
        );

        /*
         * If storage is full, remove old chat history
         * and try once more.
         */
        try {

            localStorage.removeItem(
                "hackerAnkitChatHistory"
            );

            localStorage.removeItem(
                "hackerAnkitDraft"
            );

            localStorage.setItem(
                key,
                value
            );

            return true;

        } catch (secondError) {

            console.error(
                "❌ Storage quota exceeded:",
                secondError
            );

            return false;
        }
    }
}


function safeRemove(key) {

    try {

        localStorage.removeItem(
            key
        );

    } catch (error) {

        console.warn(
            "⚠️ localStorage remove failed:",
            error
        );
    }
}


/* =========================================================
   STATE
========================================================= */

let currentUser =
    safeGet(
        "hackerAnkitUser",
        ""
    );

let currentImage = null;

let voiceEnabled =
    safeGet(
        "hackerAnkitVoice",
        "true"
    ) !== "false";

let currentChat = [];

let chatHistory = [];


/* =========================================================
   LOAD CHAT HISTORY SAFELY
========================================================= */

function loadChatHistory() {

    try {

        const raw =
            localStorage.getItem(
                "hackerAnkitChatHistory"
            );

        if (!raw) {
            return [];
        }

        const parsed =
            JSON.parse(raw);

        if (!Array.isArray(parsed)) {
            return [];
        }

        /*
         * Remove old/invalid records.
         * Also remove any Base64 image data that
         * might exist in an older version.
         */
        return parsed
            .filter(
                chat =>
                    chat &&
                    Array.isArray(
                        chat.messages
                    )
            )
            .slice(
                0,
                MAX_HISTORY_CHATS
            )
            .map(
                chat => ({

                    id:
                        chat.id ||
                        Date.now(),

                    title:
                        String(
                            chat.title ||
                            "New Chat"
                        ).slice(
                            0,
                            80
                        ),

                    createdAt:
                        chat.createdAt ||
                        Date.now(),

                    messages:
                        chat.messages
                            .slice(
                                0,
                                MAX_MESSAGES_PER_CHAT
                            )
                            .map(
                                item => ({

                                    role:
                                        item.role ===
                                        "assistant"
                                            ? "assistant"
                                            : "user",

                                    content:
                                        String(
                                            item.content ||
                                            ""
                                        ).slice(
                                            0,
                                            MAX_HISTORY_TEXT_LENGTH
                                        ),

                                    timestamp:
                                        item.timestamp ||
                                        Date.now(),

                                    /*
                                     * IMPORTANT:
                                     * Never restore Base64 images.
                                     */
                                    image: null
                                })
                            )
                })
            );

    } catch (error) {

        console.warn(
            "⚠️ Could not load chat history:",
            error
        );

        return [];
    }
}


chatHistory =
    loadChatHistory();


/* =========================================================
   INITIALIZATION
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    () => {

        updateVoiceButton();

        if (currentUser) {
            showApp();
        } else {
            showLogin();
        }

        setupTextarea();

        renderHistory();

        console.log(
            "✅ HACKER ANKIT AI frontend loaded."
        );
    }
);


/* =========================================================
   LOGIN
========================================================= */

if (loginBtn) {

    loginBtn.addEventListener(
        "click",
        login
    );
}


if (nameInput) {

    nameInput.addEventListener(
        "keydown",
        event => {

            if (event.key === "Enter") {

                event.preventDefault();

                login();
            }
        }
    );
}


function login() {

    const name =
        nameInput.value.trim();

    if (!name) {

        loginError.textContent =
            "Please enter your name.";

        nameInput.focus();

        return;
    }

    if (name.length < 2) {

        loginError.textContent =
            "Please enter at least 2 characters.";

        nameInput.focus();

        return;
    }

    currentUser =
        name.slice(
            0,
            80
        );

    safeSet(
        "hackerAnkitUser",
        currentUser
    );

    loginError.textContent =
        "";

    showApp();
}


function showLogin() {

    if (!loginScreen || !appScreen) {
        return;
    }

    loginScreen.classList.remove(
        "hidden"
    );

    appScreen.classList.add(
        "hidden"
    );

    setTimeout(
        () => {

            if (nameInput) {
                nameInput.focus();
            }

        },
        100
    );
}


function showApp() {

    if (!loginScreen || !appScreen) {
        return;
    }

    loginScreen.classList.add(
        "hidden"
    );

    appScreen.classList.remove(
        "hidden"
    );

    updateUserUI();

    if (currentChat.length === 0) {
        clearMessages();
    }
}


/* =========================================================
   USER UI
========================================================= */

function updateUserUI() {

    const firstLetter =
        currentUser.charAt(0).toUpperCase() ||
        "U";

    if (userAvatar) {

        userAvatar.textContent =
            firstLetter;
    }

    if (userNameDisplay) {

        userNameDisplay.textContent =
            currentUser || "User";
    }

    if (welcomeTitle) {

        welcomeTitle.textContent =
            `Hello ${currentUser} 👋`;
    }
}


/* =========================================================
   LOGOUT
========================================================= */

if (logoutBtn) {

    logoutBtn.addEventListener(
        "click",
        () => {

            const shouldLogout =
                confirm(
                    "Do you want to logout?"
                );

            if (!shouldLogout) {
                return;
            }

            safeRemove(
                "hackerAnkitUser"
            );

            safeRemove(
                "hackerAnkitDraft"
            );

            currentUser = "";

            currentChat = [];

            clearMessages();

            closeSidebar();

            showLogin();
        }
    );
}


/* =========================================================
   NEW CHAT
========================================================= */

if (newChatBtn) {

    newChatBtn.addEventListener(
        "click",
        startNewChat
    );
}


if (sidebarNewChatBtn) {

    sidebarNewChatBtn.addEventListener(
        "click",
        startNewChat
    );
}


function startNewChat() {

    if (currentChat.length > 0) {

        saveCurrentChatToHistory();
    }

    currentChat = [];

    clearMessages();

    removeSelectedImage();

    if (messageInput) {

        messageInput.value =
            "";
    }

    autoResizeTextarea();

    closeSidebar();

    if (messageInput) {

        messageInput.focus();
    }
}


/* =========================================================
   MESSAGE AREA
========================================================= */

function clearMessages() {

    if (!messages) {
        return;
    }

    messages.innerHTML =
        "";

    if (welcomeScreen) {

        welcomeScreen.classList.remove(
            "hidden"
        );
    }
}


function hideWelcome() {

    if (welcomeScreen) {

        welcomeScreen.classList.add(
            "hidden"
        );
    }
}


/* =========================================================
   CHAT
========================================================= */

if (sendBtn) {

    sendBtn.addEventListener(
        "click",
        sendMessage
    );
}


if (messageInput) {

    messageInput.addEventListener(
        "keydown",
        event => {

            if (
                event.key === "Enter" &&
                !event.shiftKey
            ) {

                event.preventDefault();

                sendMessage();
            }
        }
    );
}


async function sendMessage() {

    const text =
        messageInput.value.trim();

    if (!text && !currentImage) {
        return;
    }

    hideWelcome();

    const userText =
        text ||
        "Please analyze this image.";

    const imageForMessage =
        currentImage;

    /*
     * Display uploaded image on screen.
     */
    addMessage(
        "user",
        userText,
        imageForMessage
    );

    /*
     * IMPORTANT:
     * Do NOT store Base64 image inside currentChat.
     */
    currentChat.push({

        role:
            "user",

        content:
            userText,

        image:
            null,

        timestamp:
            Date.now()
    });

    messageInput.value =
        "";

    autoResizeTextarea();

    currentImage =
        null;

    if (imagePreviewBox) {

        imagePreviewBox.classList.add(
            "hidden"
        );
    }

    showTyping(true);

    try {

        const response =
            await callChatAPI(
                userText,
                imageForMessage
            );

        const answer =
            extractTextFromResponse(
                response
            );

        showTyping(false);

        addMessage(
            "assistant",
            answer
        );

        currentChat.push({

            role:
                "assistant",

            content:
                answer,

            image:
                null,

            timestamp:
                Date.now()
        });

        saveCurrentChatToHistory();

        if (voiceEnabled) {

            speakText(answer);
        }

    } catch (error) {

        console.error(
            "CHAT ERROR:",
            error
        );

        showTyping(false);

        const errorMessage =
            "❌ AI server error: " +
            (
                error.message ||
                "Unknown error"
            );

        addMessage(
            "assistant",
            errorMessage
        );
    }
}


/* =========================================================
   CHAT API
========================================================= */

async function callChatAPI(
    text,
    image
) {

    const payload = {

        name:
            currentUser,

        message:
            text,

        image:
            image
                ? image.data
                : null,

        /*
         * Send only recent text history.
         * Never send stored Base64 images.
         */
        history:
            currentChat
                .slice(-12)
                .map(
                    item => ({

                        role:
                            item.role,

                        content:
                            String(
                                item.content ||
                                ""
                            ).slice(
                                0,
                                MAX_HISTORY_TEXT_LENGTH
                            )
                    })
                )
    };

    const response =
        await fetch(
            `${API_BASE_URL}/api/chat`,
            {
                method:
                    "POST",

                headers: {

                    "Content-Type":
                        "application/json"
                },

                body:
                    JSON.stringify(
                        payload
                    )
            }
        );

    let data =
        null;

    try {

        data =
            await response.json();

    } catch {

        data =
            null;
    }

    if (!response.ok) {

        throw new Error(
            data?.error ||
            data?.message ||
            `Chat API error: ${response.status}`
        );
    }

    return data;
}


function extractTextFromResponse(
    data
) {

    if (!data) {

        return "मुझे कोई जवाब नहीं मिला।";
    }

    if (
        typeof data ===
        "string"
    ) {

        return data;
    }

    return (
        data.answer ||
        data.message ||
        data.text ||
        data.output ||
        "मुझे कोई जवाब नहीं मिला।"
    );
}


/* =========================================================
   ADD MESSAGE
========================================================= */

function addMessage(
    role,
    text,
    image = null
) {

    if (!messages) {
        return;
    }

    const message =
        document.createElement(
            "div"
        );

    message.className =
        `message ${role}`;

    const avatar =
        document.createElement(
            "div"
        );

    avatar.className =
        "message-avatar";

    avatar.textContent =
        role === "user"
            ? (
                currentUser
                    .charAt(0)
                    .toUpperCase() ||
                "U"
            )
            : "AI";

    const content =
        document.createElement(
            "div"
        );

    content.className =
        "message-content";

    if (image) {

        const img =
            document.createElement(
                "img"
            );

        img.src =
            image.data;

        img.alt =
            "Uploaded image";

        img.style.maxWidth =
            "240px";

        img.style.maxHeight =
            "260px";

        img.style.display =
            "block";

        img.style.borderRadius =
            "10px";

        img.style.marginBottom =
            "10px";

        content.appendChild(
            img
        );
    }

    const textElement =
        document.createElement(
            "div"
        );

    textElement.textContent =
        text;

    content.appendChild(
        textElement
    );

    if (role === "assistant") {

        const actions =
            document.createElement(
                "div"
            );

        actions.className =
            "message-actions";

        const copyButton =
            document.createElement(
                "button"
            );

        copyButton.className =
            "message-action";

        copyButton.textContent =
            "Copy";

        copyButton.type =
            "button";

        copyButton.addEventListener(
            "click",
            () => {

                copyText(text);
            }
        );

        const speakButton =
            document.createElement(
                "button"
            );

        speakButton.className =
            "message-action";

        speakButton.textContent =
            "🔊 Speak";

        speakButton.type =
            "button";

        speakButton.addEventListener(
            "click",
            () => {

                speakText(text);
            }
        );

        actions.appendChild(
            copyButton
        );

        actions.appendChild(
            speakButton
        );

        content.appendChild(
            actions
        );
    }

    message.appendChild(
        avatar
    );

    message.appendChild(
        content
    );

    messages.appendChild(
        message
    );

    scrollMessages();
}


/* =========================================================
   COPY
========================================================= */

async function copyText(
    text
) {

    try {

        await navigator.clipboard.writeText(
            text
        );

        showToast(
            "Copied!"
        );

    } catch (error) {

        console.error(
            error
        );

        showToast(
            "Copy failed"
        );
    }
}


/* =========================================================
   IMAGE UPLOAD / VISION
========================================================= */

if (imageInput) {

    imageInput.addEventListener(
        "change",
        handleImageUpload
    );
}


function handleImageUpload(
    event
) {

    const file =
        event.target.files[0];

    if (!file) {
        return;
    }

    if (
        !file.type.startsWith(
            "image/"
        )
    ) {

        showToast(
            "Please select an image."
        );

        imageInput.value =
            "";

        return;
    }

    const maxSize =
        10 * 1024 * 1024;

    if (file.size > maxSize) {

        showToast(
            "Image must be smaller than 10 MB."
        );

        imageInput.value =
            "";

        return;
    }

    const reader =
        new FileReader();

    reader.onload =
        () => {

            currentImage = {

                name:
                    file.name,

                type:
                    file.type,

                data:
                    reader.result
            };

            if (imagePreview) {

                imagePreview.src =
                    reader.result;
            }

            if (imagePreviewBox) {

                imagePreviewBox.classList.remove(
                    "hidden"
                );
            }
        };

    reader.onerror =
        () => {

            showToast(
                "Could not read the image."
            );
        };

    reader.readAsDataURL(
        file
    );
}


if (removeImageBtn) {

    removeImageBtn.addEventListener(
        "click",
        removeSelectedImage
    );
}


function removeSelectedImage() {

    currentImage =
        null;

    if (imageInput) {

        imageInput.value =
            "";
    }

    if (imagePreview) {

        imagePreview.src =
            "";
    }

    if (imagePreviewBox) {

        imagePreviewBox.classList.add(
            "hidden"
        );
    }
}


/* =========================================================
   MICROPHONE
========================================================= */

let recognition =
    null;

let isListening =
    false;

const SpeechRecognition =
    window.SpeechRecognition ||
    window.webkitSpeechRecognition;

if (SpeechRecognition) {

    recognition =
        new SpeechRecognition();

    recognition.lang =
        "hi-IN";

    recognition.continuous =
        false;

    recognition.interimResults =
        true;

    recognition.onstart =
        () => {

            isListening =
                true;

            if (micBtn) {

                micBtn.textContent =
                    "🔴";

                micBtn.title =
                    "Listening...";
            }
        };

    recognition.onresult =
        event => {

            let transcript =
                "";

            for (
                let i =
                    event.resultIndex;
                i <
                    event.results.length;
                i++
            ) {

                transcript +=
                    event.results[i][0]
                        .transcript;
            }

            if (messageInput) {

                messageInput.value =
                    transcript;

                autoResizeTextarea();
            }
        };

    recognition.onerror =
        event => {

            console.error(
                "Speech recognition error:",
                event.error
            );

            showToast(
                "Microphone recognition failed."
            );
        };

    recognition.onend =
        () => {

            isListening =
                false;

            if (micBtn) {

                micBtn.textContent =
                    "🎤";

                micBtn.title =
                    "Speak";
            }
        };
}


if (micBtn) {

    micBtn.addEventListener(
        "click",
        toggleMicrophone
    );
}


function toggleMicrophone() {

    if (!recognition) {

        showToast(
            "Voice input is not supported by this browser."
        );

        return;
    }

    if (isListening) {

        recognition.stop();

        return;
    }

    try {

        recognition.start();

    } catch (error) {

        console.error(
            error
        );
    }
}


/* =========================================================
   AI VOICE
========================================================= */

if (voiceToggleBtn) {

    voiceToggleBtn.addEventListener(
        "click",
        () => {

            voiceEnabled =
                !voiceEnabled;

            safeSet(
                "hackerAnkitVoice",
                String(
                    voiceEnabled
                )
            );

            updateVoiceButton();

            if (!voiceEnabled) {

                window.speechSynthesis.cancel();
            }
        }
    );
}


function updateVoiceButton() {

    if (!voiceToggleBtn) {
        return;
    }

    if (voiceEnabled) {

        voiceToggleBtn.textContent =
            "🔊";

        voiceToggleBtn.classList.add(
            "active"
        );

        voiceToggleBtn.title =
            "AI voice is ON";

    } else {

        voiceToggleBtn.textContent =
            "🔇";

        voiceToggleBtn.classList.remove(
            "active"
        );

        voiceToggleBtn.title =
            "AI voice is OFF";
    }
}


function speakText(
    text
) {

    if (
        !(
            "speechSynthesis"
            in window
        )
    ) {

        showToast(
            "Voice playback is not supported."
        );

        return;
    }

    window.speechSynthesis.cancel();

    const cleanText =
        String(text)
            .replace(
                /```[\s\S]*?```/g,
                ""
            )
            .replace(
                /[*_#`]/g,
                ""
            );

    const utterance =
        new SpeechSynthesisUtterance(
            cleanText
        );

    utterance.lang =
        "hi-IN";

    utterance.rate =
        0.95;

    utterance.pitch =
        1;

    const voices =
        window.speechSynthesis.getVoices();

    const hindiVoice =
        voices.find(
            voice =>
                voice.lang
                    .toLowerCase()
                    .includes("hi")
        );

    if (hindiVoice) {

        utterance.voice =
            hindiVoice;
    }

    window.speechSynthesis.speak(
        utterance
    );
}


/* =========================================================
   IMAGE GENERATION
========================================================= */

toolButtons.forEach(
    button => {

        button.addEventListener(
            "click",
            () => {

                const tool =
                    button.dataset.tool;

                if (
                    tool === "image"
                ) {

                    openImageModal();

                    return;
                }

                if (
                    tool === "vision"
                ) {

                    showToast(
                        "Upload an image and ask your question."
                    );

                    if (imageInput) {

                        imageInput.click();
                    }

                    return;
                }

                if (
                    tool === "chat"
                ) {

                    if (messageInput) {

                        messageInput.focus();
                    }

                    closeSidebar();
                }
            }
        );
    }
);


function openImageModal() {

    if (!imageModal) {
        return;
    }

    imageModal.classList.remove(
        "hidden"
    );

    if (imagePromptInput) {

        imagePromptInput.focus();
    }
}


function closeImageGenerator() {

    if (!imageModal) {
        return;
    }

    imageModal.classList.add(
        "hidden"
    );
}


if (closeImageModal) {

    closeImageModal.addEventListener(
        "click",
        closeImageGenerator
    );
}


if (imageModal) {

    imageModal.addEventListener(
        "click",
        event => {

            if (
                event.target ===
                imageModal
            ) {

                closeImageGenerator();
            }
        }
    );
}


if (generateImageBtn) {

    generateImageBtn.addEventListener(
        "click",
        generateImage
    );
}


/* =========================================================
   GENERATE IMAGE
========================================================= */

async function generateImage() {

    const prompt =
        imagePromptInput
            ? imagePromptInput.value.trim()
            : "";

    if (!prompt) {

        if (imageGenerationStatus) {

            imageGenerationStatus.textContent =
                "Please describe the image.";
        }

        return;
    }

    if (!generateImageBtn) {
        return;
    }

    generateImageBtn.disabled =
        true;

    generateImageBtn.textContent =
        "GENERATING...";

    if (imageGenerationStatus) {

        imageGenerationStatus.textContent =
            "🖼️ AI is creating your image...";
    }

    if (generatedImageContainer) {

        generatedImageContainer.classList.add(
            "hidden"
        );
    }

    try {

        console.log(
            "🖼️ Sending image request:",
            prompt
        );

        const response =
            await fetch(
                `${API_BASE_URL}/api/generate-image`,
                {
                    method:
                        "POST",

                    headers: {

                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify({

                            name:
                                currentUser,

                            prompt:
                                prompt
                        })
                }
            );

        let data =
            null;

        try {

            data =
                await response.json();

        } catch {

            data =
                null;
        }

        console.log(
            "🖼️ Image API response:",
            data
        );

        if (!response.ok) {

            throw new Error(
                data?.error ||
                data?.message ||
                data?.details ||
                `Image API error: ${response.status}`
            );
        }

        const imageUrl =
            data?.imageUrl ||
            data?.url ||
            data?.image ||
            (
                data?.b64_json
                    ? `data:image/png;base64,${data.b64_json}`
                    : null
            );

        if (!imageUrl) {

            throw new Error(
                "Backend ने कोई image data नहीं भेजा।"
            );
        }

        if (generatedImage) {

            generatedImage.src =
                imageUrl;
        }

        if (generatedImageContainer) {

            generatedImageContainer.classList.remove(
                "hidden"
            );
        }

        if (imageGenerationStatus) {

            imageGenerationStatus.textContent =
                "✅ Image generated successfully.";
        }

    } catch (error) {

        console.error(
            "❌ IMAGE GENERATION ERROR:",
            error
        );

        if (imageGenerationStatus) {

            imageGenerationStatus.textContent =
                "❌ Image generation failed:\n" +
                (
                    error.message ||
                    "Unknown error"
                );
        }

    } finally {

        generateImageBtn.disabled =
            false;

        generateImageBtn.textContent =
            "✨ GENERATE IMAGE";
    }
}


/* =========================================================
   QUICK PROMPTS
========================================================= */

quickPrompts.forEach(
    button => {

        button.addEventListener(
            "click",
            () => {

                const prompt =
                    button.dataset.prompt;

                if (messageInput) {

                    messageInput.value =
                        prompt;

                    autoResizeTextarea();

                    messageInput.focus();
                }
            }
        );
    }
);


/* =========================================================
   TEXTAREA
========================================================= */

function setupTextarea() {

    if (!messageInput) {
        return;
    }

    messageInput.addEventListener(
        "input",
        autoResizeTextarea
    );
}


function autoResizeTextarea() {

    if (!messageInput) {
        return;
    }

    messageInput.style.height =
        "auto";

    messageInput.style.height =
        `${Math.min(
            messageInput.scrollHeight,
            180
        )}px`;
}


/* =========================================================
   TYPING
========================================================= */

function showTyping(
    show
) {

    if (!typingIndicator) {
        return;
    }

    if (show) {

        typingIndicator.classList.remove(
            "hidden"
        );

        scrollMessages();

    } else {

        typingIndicator.classList.add(
            "hidden"
        );
    }
}


/* =========================================================
   SCROLL
========================================================= */

function scrollMessages() {

    if (!messages) {
        return;
    }

    requestAnimationFrame(
        () => {

            messages.scrollTop =
                messages.scrollHeight;
        }
    );
}


/* =========================================================
   CHAT HISTORY
========================================================= */

function saveCurrentChatToHistory() {

    if (
        currentChat.length === 0
    ) {
        return;
    }

    /*
     * Create a CLEAN copy.
     * Absolutely no Base64 image data.
     */
    const cleanMessages =
        currentChat
            .slice(
                -MAX_MESSAGES_PER_CHAT
            )
            .map(
                item => ({

                    role:
                        item.role ===
                        "assistant"
                            ? "assistant"
                            : "user",

                    content:
                        String(
                            item.content ||
                            ""
                        ).slice(
                            0,
                            MAX_HISTORY_TEXT_LENGTH
                        ),

                    timestamp:
                        item.timestamp ||
                        Date.now(),

                    image:
                        null
                })
            );

    const firstUserMessage =
        cleanMessages.find(
            item =>
                item.role === "user"
        );

    const title =
        firstUserMessage
            ? firstUserMessage.content
                .slice(
                    0,
                    60
                )
            : "New Chat";

    const chatRecord = {

        id:
            Date.now(),

        title:
            title,

        messages:
            cleanMessages,

        createdAt:
            Date.now()
    };

    /*
     * Remove duplicate record if the same
     * current chat is being saved repeatedly.
     */
    chatHistory =
        chatHistory.filter(
            chat =>
                chat.id !==
                chatRecord.id
        );

    chatHistory.unshift(
        chatRecord
    );

    chatHistory =
        chatHistory.slice(
            0,
            MAX_HISTORY_CHATS
        );

    /*
     * Try saving progressively smaller data
     * if browser storage is full.
     */
    let saved =
        safeSet(
            "hackerAnkitChatHistory",
            JSON.stringify(
                chatHistory
            )
        );

    if (!saved) {

        /*
         * Keep only last 5 chats.
         */
        chatHistory =
            chatHistory.slice(
                0,
                5
            );

        saved =
            safeSet(
                "hackerAnkitChatHistory",
                JSON.stringify(
                    chatHistory
                )
            );
    }

    if (!saved) {

        /*
         * Last fallback:
         * keep only current chat title
         * and a few text messages.
         */
        const minimalHistory = [

            {
                id:
                    chatRecord.id,

                title:
                    chatRecord.title,

                createdAt:
                    chatRecord.createdAt,

                messages:
                    cleanMessages.slice(
                        -8
                    )
            }
        ];

        chatHistory =
            minimalHistory;

        safeSet(
            "hackerAnkitChatHistory",
            JSON.stringify(
                minimalHistory
            )
        );
    }

    renderHistory();
}


function renderHistory() {

    if (!historyList) {
        return;
    }

    historyList.innerHTML =
        "";

    if (
        chatHistory.length === 0
    ) {

        const empty =
            document.createElement(
                "div"
            );

        empty.className =
            "empty-history";

        empty.textContent =
            "No chats yet";

        historyList.appendChild(
            empty
        );

        return;
    }

    chatHistory.forEach(
        chat => {

            const button =
                document.createElement(
                    "button"
                );

            button.type =
                "button";

            button.className =
                "history-item";

            button.textContent =
                chat.title ||
                "New Chat";

            button.addEventListener(
                "click",
                () => {

                    loadChat(
                        chat
                    );

                    closeSidebar();
                }
            );

            historyList.appendChild(
                button
            );
        }
    );
}


function loadChat(
    chat
) {

    currentChat =
        Array.isArray(
            chat.messages
        )
            ? chat.messages
                .map(
                    item => ({

                        role:
                            item.role ===
                            "assistant"
                                ? "assistant"
                                : "user",

                        content:
                            String(
                                item.content ||
                                ""
                            ),

                        image:
                            null,

                        timestamp:
                            item.timestamp ||
                            Date.now()
                    })
                )
            : [];

    clearMessages();

    hideWelcome();

    currentChat.forEach(
        item => {

            addMessage(
                item.role,
                item.content,
                null
            );
        }
    );

    scrollMessages();
}


/* =========================================================
   SIDEBAR
========================================================= */

if (menuBtn) {

    menuBtn.addEventListener(
        "click",
        () => {

            if (sidebar) {

                sidebar.classList.toggle(
                    "open"
                );
            }

            if (sidebarOverlay) {

                sidebarOverlay.classList.toggle(
                    "hidden"
                );
            }
        }
    );
}


if (sidebarOverlay) {

    sidebarOverlay.addEventListener(
        "click",
        closeSidebar
    );
}


function closeSidebar() {

    if (sidebar) {

        sidebar.classList.remove(
            "open"
        );
    }

    if (sidebarOverlay) {

        sidebarOverlay.classList.add(
            "hidden"
        );
    }
}


/* =========================================================
   TOAST
========================================================= */

function showToast(
    message
) {

    let toast =
        document.getElementById(
            "hackerToast"
        );

    if (!toast) {

        toast =
            document.createElement(
                "div"
            );

        toast.id =
            "hackerToast";

        Object.assign(
            toast.style,
            {

                position:
                    "fixed",

                left:
                    "50%",

                bottom:
                    "95px",

                transform:
                    "translateX(-50%)",

                zIndex:
                    "9999",

                padding:
                    "10px 16px",

                border:
                    "1px solid rgba(0,255,136,.25)",

                borderRadius:
                    "10px",

                background:
                    "rgba(10,15,22,.95)",

                color:
                    "#dce4ed",

                fontSize:
                    "12px",

                boxShadow:
                    "0 10px 40px rgba(0,0,0,.4)",

                pointerEvents:
                    "none",

                whiteSpace:
                    "pre-wrap",

                maxWidth:
                    "90%"
            }
        );

        document.body.appendChild(
            toast
        );
    }

    toast.textContent =
        message;

    clearTimeout(
        toast._timer
    );

    toast._timer =
        setTimeout(
            () => {

                toast.remove();

            },
            4000
        );
}


/* =========================================================
   KEYBOARD
========================================================= */

document.addEventListener(
    "keydown",
    event => {

        if (
            (
                event.metaKey ||
                event.ctrlKey
            ) &&
            event.key.toLowerCase() ===
                "k"
        ) {

            event.preventDefault();

            if (messageInput) {

                messageInput.focus();
            }
        }

        if (
            event.key ===
            "Escape"
        ) {

            closeImageGenerator();

            closeSidebar();
        }
    }
);


/* =========================================================
   SAVE DRAFT
========================================================= */

/*
 * IMPORTANT:
 * Do NOT save the whole currentChat automatically.
 * It can become large and cause storage quota errors.
 *
 * We only save the last 3 TEXT messages.
 */
window.addEventListener(
    "beforeunload",
    () => {

        try {

            if (
                currentChat.length === 0
            ) {
                return;
            }

            const smallDraft =
                currentChat
                    .slice(
                        -3
                    )
                    .map(
                        item => ({

                            role:
                                item.role,

                            content:
                                String(
                                    item.content ||
                                    ""
                                ).slice(
                                    0,
                                    2000
                                )
                        })
                    );

            safeSet(
                "hackerAnkitDraft",
                JSON.stringify(
                    smallDraft
                )
            );

        } catch (error) {

            console.warn(
                "⚠️ Draft save skipped:",
                error
            );
        }
    }
);


/* =========================================================
   CLEAN OLD STORAGE
========================================================= */

/*
 * This runs once when the app loads.
 * It removes old oversized draft data.
 */
(function cleanupOldStorage() {

    try {

        const history =
            loadChatHistory();

        /*
         * Re-save cleaned history.
         */
        if (history.length > 0) {

            chatHistory =
                history;

            safeSet(
                "hackerAnkitChatHistory",
                JSON.stringify(
                    history
                )
            );
        }

        /*
         * Old drafts are not needed anymore.
         */
        safeRemove(
            "hackerAnkitDraft"
        );

    } catch (error) {

        console.warn(
            "⚠️ Storage cleanup skipped:",
            error
        );
    }

})();


/* =========================================================
   FINAL
========================================================= */

console.log(
    "✅ HACKER ANKIT AI frontend loaded successfully."
);

console.log(
    "✅ Storage-safe mode enabled."
);
````
