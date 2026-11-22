// ===== CHINU OS v2.0 - Natural Female Voice + Jarvis Core =====

const orbBtn = document.getElementById("orb-btn");
const micBtn = document.getElementById("mic-btn");
const sendBtn = document.getElementById("send-btn");
const flashBtn = document.getElementById("flash-btn");
const textInput = document.getElementById("text-input");
const chatLog = document.getElementById("chat-log");
const statusText = document.getElementById("status-text");
const liveTime = document.getElementById("live-time");
const liveDate = document.getElementById("live-date");
const batteryVal = document.getElementById("battery-val");
const netVal = document.getElementById("net-val");

const settingsBtn = document.getElementById("settings-btn");
const settingsModal = document.getElementById("settings-modal");
const apiKeyInput = document.getElementById("api-key-input");
const saveSettings = document.getElementById("save-settings");
const closeSettings = document.getElementById("close-settings");

let isFlashOn = false;
let selectedVoice = null;
let geminiApiKey = localStorage.getItem("chinu_api_key") || "";

// ===== 1. LIVE CLOCK & DATE =====
function updateClock() {
  const now = new Date();
  liveTime.textContent = now.toLocaleTimeString("en-IN", { hour12: false });
  liveDate.textContent = now.toLocaleDateString("en-IN", {
    weekday: "short",
    day: "2-digit",
    month: "short",
    year: "numeric"
  }).toUpperCase();
}
setInterval(updateClock, 1000);
updateClock();

// ===== 2. BATTERY & NETWORK STATUS =====
if (navigator.getBattery) {
  navigator.getBattery().then((battery) => {
    const updateBat = () => {
      batteryVal.textContent = Math.round(battery.level * 100) + "%";
    };
    updateBat();
    battery.addEventListener("levelchange", updateBat);
  });
}

function updateNetwork() {
  netVal.textContent = navigator.onLine ? "ONLINE" : "OFFLINE";
  netVal.style.color = navigator.onLine ? "#4dff88" : "#ff4d5e";
}
window.addEventListener("online", updateNetwork);
window.addEventListener("offline", updateNetwork);
updateNetwork();

// ===== 3. NATURAL 20+ FEMALE VOICE ENGINE =====
function loadFemaleVoice() {
  const voices = window.speechSynthesis.getVoices();
  if (!voices.length) return;

  // Priority: Natural Indian English / Hindi Female voices (Mature 20+ tone)
  const preferredNames = [
    "Google हिन्दी",
    "Google English (India)",
    "en-in-x-enc-network",
    "en-in-x-ene-network",
    "en-in-x-enc-local",
    "hi-in-x-hia-network",
    "hi-in-x-hic-network",
    "Samantha",
    "Zira",
    "Female"
  ];

  for (const name of preferredNames) {
    const found = voices.find(v => v.name.toLowerCase().includes(name.toLowerCase()));
    if (found) {
      selectedVoice = found;
      return;
    }
  }

  // Fallback to any en-IN or hi-IN or female-sounding voice
  selectedVoice =
    voices.find(v => v.lang === "en-IN") ||
    voices.find(v => v.lang === "hi-IN") ||
    voices.find(v => v.lang.startsWith("en")) ||
    voices[0];
}

window.speechSynthesis.onvoiceschanged = loadFemaleVoice;
loadFemaleVoice();

function speak(text) {
  window.speechSynthesis.cancel();
  const utter = new SpeechSynthesisUtterance(text);
  if (!selectedVoice) loadFemaleVoice();
  if (selectedVoice) utter.voice = selectedVoice;

  // Natural 20+ adult female voice settings (not robotic, not child-like)
  utter.pitch = 1.05;
  utter.rate = 0.98;
  utter.volume = 1.0;

  utter.onstart = () => {
    orbBtn.classList.add("speaking");
    statusText.textContent = "CHINU IS SPEAKING...";
  };
  utter.onend = () => {
    orbBtn.classList.remove("speaking");
    statusText.textContent = "TAP ORB TO SPEAK";
  };

  window.speechSynthesis.speak(utter);
}

// ===== 4. CHAT LOG HELPER =====
function addMessage(sender, text) {
  const div = document.createElement("div");
  div.className = `msg ${sender}`;
  div.innerHTML = `<b>${sender === "ai" ? "CHINU" : "YOU"}:</b> ${text}`;
  chatLog.appendChild(div);
  chatLog.scrollTop = chatLog.scrollHeight;
}

// ===== 5. FLASHLIGHT CONTROL (NATIVE ANDROID BRIDGE) =====
function toggleFlashlight(state) {
  isFlashOn = typeof state === "boolean" ? state : !isFlashOn;
  if (window.ChinuNative && typeof window.ChinuNative.setFlashlight === "function") {
    window.ChinuNative.setFlashlight(isFlashOn);
    return true;
  }
  return false;
}

flashBtn.addEventListener("click", () => {
  const ok = toggleFlashlight();
  const reply = ok
    ? (isFlashOn ? "Flashlight on kar di hai." : "Flashlight band kar di hai.")
    : "Flashlight sirf CHINU APK app mein kaam karegi.";
  addMessage("ai", reply);
  speak(reply);
});

// ===== 6. JARVIS COMMAND PROCESSOR =====
async function handleCommand(rawCmd) {
  const cmd = rawCmd.trim();
  if (!cmd) return;

  addMessage("user", cmd);
  const lower = cmd.toLowerCase();

  // --- Flashlight ---
  if (lower.includes("flashlight on") || lower.includes("torch on") || lower.includes("light chalu") || lower.includes("flash on")) {
    toggleFlashlight(true);
    const reply = "Done, flashlight on kar di hai.";
    addMessage("ai", reply);
    speak(reply);
    return;
  }
  if (lower.includes("flashlight off") || lower.includes("torch off") || lower.includes("light band") || lower.includes("flash off")) {
    toggleFlashlight(false);
    const reply = "Okay, flashlight off kar di hai.";
    addMessage("ai", reply);
    speak(reply);
    return;
  }

  // --- Identity & Greetings ---
  if (lower.includes("who are you") || lower.includes("tum kaun ho") || lower.includes("tamaru naam") || lower.includes("your name")) {
    const reply = "Main CHINU hoon, aapki personal AI assistant. Aap mujhe kuch bhi pooch sakte hain.";
    addMessage("ai", reply);
    speak(reply);
    return;
  }
  if (lower === "hi" || lower === "hello" || lower === "hey chinu" || lower === "chinu") {
    const reply = "Hello! Main sun rahi hoon, bataiye kya kaam karna hai?";
    addMessage("ai", reply);
    speak(reply);
    return;
  }

  // --- Time & Date ---
  if (lower.includes("time") || lower.includes("samay") || lower.includes("kitne baje")) {
    const t = new Date().toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: true });
    const reply = `Abhi ${t} baje hain.`;
    addMessage("ai", reply);
    speak(reply);
    return;
  }
  if (lower.includes("date") || lower.includes("tarikh") || lower.includes("aaj kya din")) {
    const d = new Date().toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
    const reply = `Aaj ${d} hai.`;
    addMessage("ai", reply);
    speak(reply);
    return;
  }

  // --- Battery ---
  if (lower.includes("battery")) {
    const reply = `Aapke phone ki battery abhi ${batteryVal.textContent} hai.`;
    addMessage("ai", reply);
    speak(reply);
    return;
  }

  // --- YouTube Play / Open ---
  if (lower.startsWith("play ") || lower.includes("youtube par")) {
    const query = lower.replace("play ", "").replace("youtube par", "").trim();
    const reply = `YouTube par ${query} play kar rahi hoon.`;
    addMessage("ai", reply);
    speak(reply);
    window.open(`https://www.youtube.com/results?search_query=${encodeURIComponent(query)}`, "_blank");
    return;
  }
  if (lower.includes("open youtube")) {
    const reply = "YouTube khol rahi hoon.";
    addMessage("ai", reply);
    speak(reply);
    window.open("https://www.youtube.com", "_blank");
    return;
  }

  // --- Open Apps / Websites ---
  if (lower.includes("open whatsapp")) {
    const reply = "WhatsApp khol rahi hoon.";
    addMessage("ai", reply);
    speak(reply);
    window.open("https://wa.me/", "_blank");
    return;
  }
  if (lower.includes("open instagram")) {
    const reply = "Instagram khol rahi hoon.";
    addMessage("ai", reply);
    speak(reply);
    window.open("https://www.instagram.com", "_blank");
    return;
  }
  if (lower.startsWith("search ") || lower.startsWith("google ")) {
    const q = lower.replace("search ", "").replace("google ", "").trim();
    const reply = `Google par ${q} search kar rahi hoon.`;
    addMessage("ai", reply);
    speak(reply);
    window.open(`https://www.google.com/search?q=${encodeURIComponent(q)}`, "_blank");
    return;
  }

  // --- Simple Calculator ---
  if (/^[\d\s+\-*/().]+$/.test(lower)) {
    try {
      const result = Function(`"use strict"; return (${lower})`)();
      const reply = `Iska answer ${result} hai.`;
      addMessage("ai", reply);
      speak(reply);
      return;
    } catch (e) {}
  }

  // --- Online Smart AI (Gemini API if configured) ---
  if (geminiApiKey && navigator.onLine) {
    statusText.textContent = "THINKING...";
    try {
      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${geminiApiKey}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [{
              parts: [{
                text: `You are CHINU, a smart, warm, 22-year-old female AI assistant like Jarvis. Reply briefly in 1-2 natural sentences in the same language (Hinglish/Gujarati/English) as the user. User said: ${cmd}`
              }]
            }]
          })
        }
      );
      const data = await res.json();
      const aiReply = data?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (aiReply) {
        const cleanReply = aiReply.replace(/\*/g, "").trim();
        addMessage("ai", cleanReply);
        speak(cleanReply);
        return;
      }
    } catch (err) {
      console.error(err);
    }
  }

  // --- Smart Fallback (Google Search) ---
  const fallback = `Mujhe iske baare mein exact jaankari nahi mili, main Google par "${cmd}" search kar rahi hoon.`;
  addMessage("ai", fallback);
  speak(fallback);
  window.open(`https://www.google.com/search?q=${encodeURIComponent(cmd)}`, "_blank");
}

// ===== 7. VOICE RECOGNITION (SPEECH TO TEXT) =====
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;

if (SpeechRecognition) {
  recognition = new SpeechRecognition();
  recognition.lang = "en-IN";
  recognition.interimResults = false;
  recognition.maxAlternatives = 1;

  recognition.onstart = () => {
    orbBtn.classList.add("listening");
    statusText.textContent = "LISTENING...";
  };

  recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    handleCommand(transcript);
  };

  recognition.onerror = () => {
    orbBtn.classList.remove("listening");
    statusText.textContent = "TAP ORB TO SPEAK";
  };

  recognition.onend = () => {
    orbBtn.classList.remove("listening");
    if (!orbBtn.classList.contains("speaking")) {
      statusText.textContent = "TAP ORB TO SPEAK";
    }
  };
}

function startListening() {
  if (!recognition) {
    const msg = "Voice mic is device mein support nahi ho raha, kripya type karein.";
    addMessage("ai", msg);
    speak(msg);
    return;
  }
  try {
    window.speechSynthesis.cancel();
    recognition.start();
  } catch (e) {}
}

orbBtn.addEventListener("click", startListening);
micBtn.addEventListener("click", startListening);

// ===== 8. TEXT INPUT & SETTINGS =====
sendBtn.addEventListener("click", () => {
  handleCommand(textInput.value);
  textInput.value = "";
});

textInput.addEventListener("keypress", (e) => {
  if (e.key === "Enter") {
    handleCommand(textInput.value);
    textInput.value = "";
  }
});

settingsBtn.addEventListener("click", () => {
  apiKeyInput.value = geminiApiKey;
  settingsModal.classList.remove("hidden");
});

closeSettings.addEventListener("click", () => {
  settingsModal.classList.add("hidden");
});

saveSettings.addEventListener("click", () => {
  geminiApiKey = apiKeyInput.value.trim();
  localStorage.setItem("chinu_api_key", geminiApiKey);
  settingsModal.classList.add("hidden");
  const msg = "Settings save ho gayi hain.";
  addMessage("ai", msg);
  speak(msg);
});
  
