# SkyVision 🌤️


## Demo

https://github.com/user-attachments/assets/12cb32dc-d1c1-42c0-b1be-20e2ec413059


## Screenshots

![Results](./screenshots/Results.png)
![Share](./screenshots/Share.png)

> AI-powered weather intelligence — real weather, real descriptions, real imagery.

SkyVision is a full-stack weather application that combines live weather data, locally-hosted AI language models, and AI image generation to create a unique atmospheric experience for any city in the world.

---

## Demo

Search any city → get live weather data → receive an AI-written description → watch a photorealistic AI-generated cityscape load in real time.

---

## Features

- **Live weather data** via OpenWeatherMap API (temperature, humidity, wind, feels like)
- **AI weather descriptions** powered by Llama3 running locally via Ollama
- **AI-generated city imagery** via ComfyUI + SDXL — unique photorealistic scenes generated for each search
- **Image slideshow** — 2 unique images generated per city and cycled automatically
- **Exact local time** — displays the precise local time at the searched city
- **°F / °C toggle** and **12hr / 24hr toggle** for user preferences
- **City autocomplete** — 12,000+ cities with keyboard navigation
- **Shareable results** — generate a unique URL for any weather result (images included)
- **Search history** — last 8 searches saved locally with thumbnails
- **Animated landmark decorations** — 10 iconic world landmarks float around the page

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.4.5 |
| AI Language Model | Llama3 via Ollama (local) |
| AI Image Generation | ComfyUI + SDXL Base 1.0 (local) |
| Weather Data | OpenWeatherMap API |
| Database | H2 (embedded, file-based) |
| Frontend | Vanilla HTML/CSS/JavaScript |
| Build Tool | Maven |

---

## Architecture

```
Browser
  │
  ▼
Spring Boot (port 8081)
  ├── /weather/full     → OpenWeatherMap API
  ├── /weather/image    → Ollama (11434) → ComfyUI (8188)
  ├── /share            → H2 Database
  └── /static/index.html → Served directly
```

**Port layout on local machine:**
- `8081` — Spring Boot (this app)
- `8080` — Open WebUI
- `8188` — ComfyUI
- `11434` — Ollama

---

## Prerequisites

- Java 21+
- Maven
- [Ollama](https://ollama.ai) with `llama3` pulled
- [ComfyUI](https://github.com/comfyanonymous/ComfyUI) with SDXL Base 1.0 checkpoint
- OpenWeatherMap API key (free tier works)

---

## Installation

**1. Clone the repository**
```bash
git clone https://github.com/Ali-Jabbar-CS/ai-weather-assistant.git
cd ai-weather-assistant
```

**2. Add your API key**

Copy the example properties file and fill in your key:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties`:
```properties
weather.api.key=YOUR_OPENWEATHERMAP_KEY
comfyui.url=http://localhost:8188
comfyui.checkpoint=sd_xl_base_1.0.safetensors
```

**3. Start Ollama**
```bash
ollama serve
ollama pull llama3
```

**4. Start ComfyUI (WSL)**
```bash
cd ~/ComfyUI
./venv/bin/python main.py
```

**5. Build and run**
```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

**6. Open the app**

Navigate to `http://localhost:8081`

---

## Project Structure

```
src/main/java/com/ali/ai_weather_assistant/
├── controller/
│   ├── WeatherController.java   # Main API endpoints
│   └── ShareController.java     # Share feature endpoints
├── model/
│   ├── WeatherData.java         # Parsed weather model
│   └── SharedResult.java        # JPA entity for shared results
├── repository/
│   └── ShareRepository.java     # H2 database access
└── service/
    ├── WeatherService.java      # OpenWeatherMap integration
    ├── AIService.java           # Ollama/Llama3 integration
    └── ComfyUIService.java      # ComfyUI workflow submission

src/main/resources/
├── static/
│   └── index.html               # Full frontend (HTML + CSS + JS)
└── application.properties       # Configuration
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/weather?city=London` | Raw weather JSON |
| GET | `/weather/describe?city=London` | AI weather description |
| GET | `/weather/image?city=London` | Generated PNG image |
| GET | `/weather/full?city=London` | Full weather data + description |
| POST | `/share` | Save a result, returns `{ id }` |
| GET | `/share/{id}` | Retrieve a saved result |

---

## How Image Generation Works

1. Live weather data is fetched from OpenWeatherMap
2. Llama3 generates a photorealistic image prompt based on city, weather conditions, time of day, and season
3. The prompt is submitted to ComfyUI as an API workflow
4. Spring Boot polls ComfyUI every 2 seconds until the image is ready
5. The image bytes are returned directly to the browser
6. Two images are generated per search and cycled in a slideshow

---

## Milestones

| Version | Feature |
|---|---|
| v0.1 | Core weather + AI endpoints |
| v0.2 | ComfyUI integration |
| v0.3 | Image quality improvements |
| v0.4 | Context-aware AI prompting |
| v0.5 | SkyVision frontend |
| v0.6 | UI polish and animations |
| v0.7 | Image slideshow cycling |
| v0.8 | Landmark redesign |
| v0.9 | Exact local time + accuracy |
| v1.0 | Share + search history |

---

## Key Technical Decisions

**Why local AI instead of cloud APIs?**
Running Llama3 and SDXL locally demonstrates understanding of self-hosted AI infrastructure, avoids API costs, and keeps all data private.

**Why Spring Boot over Node/Python?**
Java's strong typing and Spring's dependency injection make the service layer clean and testable. Spring Boot's embedded Tomcat means zero deployment configuration.

**Why H2 for the share feature?**
H2 is a zero-configuration embedded database that persists to a file. It demonstrates JPA/Hibernate knowledge without requiring a separate database server, making the project fully self-contained.

**Why vanilla JS on the frontend?**
The frontend has meaningful complexity (slideshow, autocomplete, toggles, share logic) implemented without a framework — demonstrating strong core JavaScript skills.

---

## Author

**Ali Jabbar**

- 🔗 [GitHub](https://github.com/Ali-Jabbar-CS)
- 💼 [LinkedIn](https://www.linkedin.com/in/ali-jabbar-8b22b5209)
- 📧 alimazin904@gmail.com
