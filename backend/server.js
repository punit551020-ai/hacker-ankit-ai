import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import path from "path";
import { fileURLToPath } from "url";
import { GoogleGenAI } from "@google/genai";

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

if (!process.env.GEMINI_API_KEY) {
    console.error("❌ GEMINI_API_KEY नहीं मिली।");
    process.exit(1);
}

const ai = new GoogleGenAI({
    apiKey: process.env.GEMINI_API_KEY
});

console.log("✅ Gemini API key loaded");

app.use(cors());

app.use(express.json({
    limit: "20mb"
}));

const frontendPath = path.join(
    __dirname,
    "..",
    "frontend"
);

console.log("Frontend path:", frontendPath);

app.use(express.static(frontendPath));

app.post("/api/chat", async (req, res) => {
    try {
        const {
            name = "User",
            message = "",
            image = null,
            history = []
        } = req.body;

        if (!message && !image) {
            return res.status(400).json({
                success: false,
                error: "Message या image जरूरी है।"
            });
        }

        const contents = [];

        if (Array.isArray(history)) {
            for (const item of history.slice(-12)) {
                if (
                    item &&
                    (
                        item.role === "user" ||
                        item.role === "assistant"
                    ) &&
                    typeof item.content === "string"
                ) {
                    contents.push({
                        role:
                            item.role === "assistant"
                                ? "model"
                                : "user",
                        parts: [
                            {
                                text: item.content
                            }
                        ]
                    });
                }
            }
        }

        const currentParts = [];

        currentParts.push({
            text:
                message ||
                "इस image को analyze करके बताओ।"
        });

        if (image) {
            let mimeType = "image/jpeg";
            let base64Data = image;

            if (typeof image === "string") {
                const match = image.match(
                    /^data:(image\/[^;]+);base64,(.+)$/
                );

                if (match) {
                    mimeType = match[1];
                    base64Data = match[2];
                }
            }

            currentParts.push({
                inlineData: {
                    mimeType,
                    data: base64Data
                }
            });
        }

        contents.push({
            role: "user",
            parts: currentParts
        });

        const response = await ai.models.generateContent({
            model: "gemini-3.6-flash",
            contents,
            config: {
                systemInstruction: `
You are HACKER ANKIT AI.

The user's name is ${name}.

You are a helpful and intelligent AI assistant.

Answer naturally in Hindi, Hinglish, or English
according to the user's language.

Keep answers clear, useful and easy to understand.
`,
                temperature: 0.7
            }
        });

        const answer =
            response.text ||
            "मुझे अभी कोई जवाब नहीं मिला।";

        console.log("✅ Chat response generated");

        res.json({
            success: true,
            answer
        });

    } catch (error) {
        console.error("❌ CHAT ERROR:", error);

        res.status(500).json({
            success: false,
            error: "AI response लेने में समस्या हुई।",
            details: error.message
        });
    }
});

app.post("/api/generate-image", async (req, res) => {
    try {
        const {
            prompt = ""
        } = req.body;

        if (!prompt.trim()) {
            return res.status(400).json({
                success: false,
                error: "Image prompt जरूरी है।"
            });
        }

        console.log("🎨 Generating image:", prompt);

        const response =
            await ai.models.generateContent({
                model: "gemini-3.1-flash-image",
                contents: prompt.trim(),
                config: {
                    responseModalities: ["IMAGE"]
                }
            });

        let imageData = null;
        let mimeType = "image/png";

        const parts =
            response.candidates?.[0]?.content?.parts || [];

        for (const part of parts) {
            if (part.inlineData) {
                imageData = part.inlineData.data;
                mimeType =
                    part.inlineData.mimeType ||
                    "image/png";
                break;
            }
        }

        if (!imageData) {
            return res.status(500).json({
                success: false,
                error: "Gemini ने image generate नहीं की।"
            });
        }

        console.log("✅ Image generated successfully");

        res.json({
            success: true,
            imageUrl:
                `data:${mimeType};base64,${imageData}`
        });

    } catch (error) {
        console.error(
            "❌ IMAGE GENERATION ERROR:",
            error
        );

        res.status(500).json({
            success: false,
            error:
                "Image generation में समस्या हुई।",
            details: error.message
        });
    }
});

app.get("/api/health", (req, res) => {
    res.json({
        success: true,
        message: "HACKER ANKIT AI is running"
    });
});

app.get("*", (req, res) => {
    res.sendFile(
        path.join(
            frontendPath,
            "index.html"
        )
    );
});

app.listen(PORT, () => {
    console.log("");
    console.log("====================================");
    console.log("   HACKER ANKIT AI SERVER");
    console.log("====================================");
    console.log(
        `✅ Server running: http://localhost:${PORT}`
    );
    console.log("✅ Chat API: /api/chat");
    console.log("✅ Image API: /api/generate-image");
    console.log("====================================");
    console.log("");
});