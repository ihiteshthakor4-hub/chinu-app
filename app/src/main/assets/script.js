document.addEventListener("DOMContentLoaded", () => {
    const orb = document.querySelector("#orb, .orb, .orb-container");
    const micBtn = document.querySelector("#mic-btn, #micBtn, .mic-btn");
    const sendBtn = document.querySelector("#send-btn, #sendBtn, .send-btn");
    const flashBtn = document.querySelector("#flash-btn, #flashBtn, .flash-btn");
    const input = document.querySelector("#user-input, #userInput, input[type='text']");
    const chatBox = document.querySelector("#chat-box, #chatBox, .chat-box, #output");
    const statusText = document.querySelector("#status, .status-text, #orb-status");

    // Clock & Date Update
    function updateClock() {
        const now = new Date();
        const timeEl = document.querySelector("#time, #clock, .time");
        const dateEl = document.querySelector("#date, .date");
        if (timeEl) timeEl.textContent = now.toTimeString().split(" ")[0];
        if (dateEl) dateEl.textContent = now.toDateString().toUpperCase();
    }
    setInterval(updateClock, 1000);
    updateClock();

    // Battery Update
    if (navigator.getBattery) {
        navigator.getBattery().then(b => {
            const batEl = document.querySelector("#battery, #battery-level, .battery");
            const updateBat = () => { if (batEl) batEl.textContent = Math.round(b.level * 100) + "%"; };
            updateBat();
            b.addEventListener("levelchange", updateBat);
        });
    }

    function showReply(text) {
        if (chatBox) {
            chatBox.innerHTML = `<div class="msg chinu"><b>CHINU:</b> ${text}</div>`;
        }
        if (window.Android && Android.speak) {
            Android.speak(text);
        } else if ("speechSynthesis" in window) {
            const u = new SpeechSynthesisUtterance(text);
            u.lang = "en-IN";
            window.speechSynthesis.speak(u);
        }
    }

    // Listening UI Callbacks from Android
    window.onListeningStart = () => {
        if (statusText) statusText.textContent = "LISTENING...";
        if (orb) orb.classList.add("listening");
    };

    window.onListeningEnd = () => {
        if (statusText) statusText.textContent = "TAP ORB TO SPEAK";
        if (orb) orb.classList.remove("listening");
    };

    window.onVoiceResult = (spokenText) => {
        handleCommand(spokenText);
    };

    function startVoice() {
        if (window.Android && Android.startListening) {
            Android.startListening();
        } else {
            showReply("Voice recognition is ready on the Android app.");
        }
    }

    function handleCommand(rawText) {
        const cmd = rawText.trim().toLowerCase();
        if (!cmd) return;

        // 1. Flashlight / Torch
        if (cmd.includes("torch") || cmd.includes("flash") || cmd.includes("light")) {
            if (window.Android && Android.toggleFlashlight) {
                const isOn = Android.toggleFlashlight();
                showReply(isOn ? "Flashlight chalu kari didhi chhe." : "Flashlight bandh kari didhi chhe.");
            } else {
                showReply("Flashlight toggle thai gai chhe.");
            }
            return;
        }

        // 2. Open Apps (WhatsApp, YouTube, Camera, Instagram, Settings, Phone, Chrome)
        const apps = ["whatsapp", "youtube", "instagram", "insta", "camera", "settings", "setting", "chrome", "phone", "call", "dialer"];
        for (const app of apps) {
            if (cmd.includes(app) && (cmd.includes("open") || cmd.includes("khol") || cmd.includes("chalu") || cmd.includes("start") || cmd === app)) {
                if (window.Android && Android.openApp) {
                    const ok = Android.openApp(app);
                    showReply(ok ? `${app} kholi rahyu chhu.` : `Tamara phone ma ${app} nathi malyu.`);
                }
                return;
            }
        }

        // 3. Time & Date
        if (cmd.includes("time") || cmd.includes("samay") || cmd.includes("ketla vagya")) {
            const t = new Date().toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
            showReply(`Atyare ${t} vagya chhe.`);
            return;
        }
        if (cmd.includes("date") || cmd.includes("tarikh") || cmd.includes("aaje kayo var")) {
            const d = new Date().toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "long" });
            showReply(`Aaje ${d} chhe.`);
            return;
        }

        // 4. Explicit Google Search ONLY when asked
        if (cmd.startsWith("search ") || cmd.includes("google par search") || cmd.includes("google search")) {
            const q = cmd.replace("google par search", "").replace("google search", "").replace("search", "").trim();
            if (q && window.Android && Android.searchGoogle) {
                showReply(`Google par ${q} search karu chhu.`);
                Android.searchGoogle(q);
            } else {
                showReply("Su search karvu chhe e to kaho!");
            }
            return;
        }

        // 5. Offline Friendly & Empathetic Conversation
        if (cmd.includes("hello") || cmd.includes("hi") || cmd.includes("hey") || cmd.includes("namaste") || cmd.includes("kem cho") || cmd.includes("kem chho")) {
            showReply("Hello! Hu ekdam majama chhu. Bolo, aaje tamara mate su kam karu?");
            return;
        }
        if (cmd.includes("good morning")) {
            showReply("Good morning! Aajno divas tamaro khub saras jay.");
            return;
        }
        if (cmd.includes("good night")) {
            showReply("Good night! Aaram karo, kale pacha malishu.");
            return;
        }
        if (cmd.includes("taru naam") || cmd.includes("your name") || cmd.includes("who are you") || cmd.includes("tu kon")) {
            showReply("Maru naam CHINU chhe. Hu tamari personal AI assistant chhu.");
            return;
        }
        if (cmd.includes("sad") || cmd.includes("mood kharab") || cmd.includes("tension") || cmd.includes("thaki gayo") || cmd.includes("stress")) {
            showReply("Chinta na karo, hu tamari sathe j chhu. Thodo aaram kari lo, badhu barobar thai jase.");
            return;
        }
        if (cmd.includes("thank") || cmd.includes("shukriya") || cmd.includes("aabhar")) {
            showReply("Welcome! Tamari madad karine mane hamesha khushi thay chhe.");
            return;
        }
        if (cmd.includes("joke") || cmd.includes("hasav")) {
            showReply("Ek vaar mobile ne charge par mukyo, pan switch on karvanu bhuli gaya... pachi khabar padi ke jindagi ma pan ghani vaar aavu j thay chhe!");
            return;
        }

        // 6. Default Offline Reply (No random Google Search!)
        showReply(`Me tamari vaat sambhali: "${rawText}". Tame mane flashlight, time, WhatsApp, YouTube ke Camera kholva kahi shako chho — athva Google mate "search" boline kaho.`);
    }

    // Button Event Listeners
    if (orb) orb.addEventListener("click", startVoice);
    if (micBtn) micBtn.addEventListener("click", startVoice);
    if (flashBtn) flashBtn.addEventListener("click", () => handleCommand("flashlight"));
    if (sendBtn && input) {
        sendBtn.addEventListener("click", () => {
            handleCommand(input.value);
            input.value = "";
        });
        input.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                handleCommand(input.value);
                input.value = "";
            }
        });
    }
});
