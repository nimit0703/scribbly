# Scribb - Real-time Drawing Game

A real-time multiplayer drawing and guessing game built with Spring Boot backend and Vue.js frontend. Players take turns drawing words while others guess in real-time.

## 🎮 Features

- **Real-time Drawing**: Live canvas drawing with WebSocket communication
- **Multiplayer Gameplay**: Support for multiple players in game rooms
- **Turn-based System**: Rotating drawer selection with automatic round management
- **Word Selection**: Random word generation with multiple choice options
- **Scoring System**: Points awarded for correct guesses and successful drawings
- **Chat Integration**: Real-time chat with automatic word detection
- **Timer System**: Configurable round timers with automatic round ending
- **Game State Management**: Persistent game rooms and player management

## 🏗️ Architecture

### Backend (Spring Boot)
- **WebSocket Communication**: Real-time bidirectional communication
- **Room Management**: Dynamic room creation and player management
- **Game Logic**: Turn rotation, scoring, and round management
- **Timer Service**: Automated round timing and scheduling
- **Word Service**: Random word generation and validation

### Frontend (Vue.js)
- **Canvas Drawing**: Interactive drawing interface
- **Real-time Updates**: Live game state synchronization
- **Chat System**: Integrated chat with guess detection
- **Responsive UI**: Modern, mobile-friendly interface
- **Game Controls**: Intuitive game management interface

## 🛠️ Technology Stack

### Backend
- **Spring Boot** - Application framework
- **Spring WebSocket** - Real-time communication
- **Spring Messaging** - WebSocket message handling
- **Lombok** - Code generation
- **Java** - Programming language

### Frontend
- **Vue.js** - Frontend framework
- **WebSocket API** - Real-time communication
- **HTML5 Canvas** - Drawing functionality
- **CSS3** - Styling and animations
- **JavaScript** - Client-side logic

## 🚀 Getting Started

### Prerequisites
- Java 11 or higher
- Node.js 14 or higher
- Maven 3.6 or higher
- npm or yarn
- Docker and Docker Compose (optional, for containerized setup)

### Quick Start (Both Backend & Frontend)

**Option 1: Using Docker Compose (Recommended)**

```bash
# Clone the repository
git clone <repository-url>
cd scribbly

# Navigate to backend directory where docker-compose.yml is located
cd backend/game

# Start both services
docker-compose up -d

# Backend will be available at http://localhost:8080
# Frontend will be available at http://localhost:3000
```
**Option 2: Running Locally in Separate Terminals**

Terminal 1 - Backend:
```bash
cd backend/game
.\mvnw clean install
.\mvnw spring-boot:run
```

Terminal 2 - Frontend:
```bash
cd frontend
npm install
npm run dev
```

Then access the application at `http://localhost:3000`

### Backend Setup (Detailed)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd scribbly
   ```

2. **Navigate to backend directory**
   ```bash
   cd backend/game
   ```

3. **Install dependencies and build**
   ```bash
   .\mvnw clean install
   ```

4. **Run the application**
   ```bash
   .\mvnw spring-boot:run
   ```

The backend will start on `http://localhost:8080`

### Frontend Setup (Detailed)

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Run the development server**
   ```bash
   npm run dev
   ```

The frontend will start on `http://localhost:3000`

### Accessing the Application

- **Frontend**: Open your browser and navigate to `http://localhost:3000`
- **Backend API**: `http://localhost:8080`
- **WebSocket**: `ws://localhost:8080/ws`

## 🎯 Game Flow

1. **Room Creation**: Players join or create game rooms
2. **Player Joining**: Minimum 2 players required to start
3. **Word Selection**: Current drawer selects from 3 random words
4. **Drawing Phase**: Drawer creates artwork while others guess
5. **Guessing Phase**: Players submit guesses via chat
6. **Scoring**: Points awarded for correct guesses
7. **Round Rotation**: Next player becomes drawer
8. **Game End**: Game concludes after all players have drawn

## 🔧 WebSocket Endpoints

### Client → Server Messages
- `/join` - Join a game room
- `/draw` - Send drawing data
- `/draw-start` - Start drawing stroke
- `/draw-end` - End drawing stroke
- `/clear` - Clear canvas
- `/chat` - Send chat message/guess
- `/word-select` - Select word to draw
- `/start-round` - Start new round
- `/end-round` - End current round
- `/timer-ended` - Timer expired

### Server → Client Messages
- `/topic/players/{roomId}` - Player list updates
- `/topic/draw/{roomId}` - Drawing data
- `/topic/draw-start/{roomId}` - Drawing start events
- `/topic/draw-end/{roomId}` - Drawing end events
- `/topic/clear/{roomId}` - Canvas clear events
- `/topic/chat/{roomId}` - Chat messages
- `/topic/system/{roomId}` - System messages
- `/topic/word-options/{username}` - Word choices for drawer
- `/topic/hint/{roomId}` - Masked word hints
- `/topic/drawer/{roomId}` - Current drawer updates
- `/topic/round/{roomId}` - Round number updates

## ⚙️ Configuration

### Backend Configuration (Not required at all)
```properties
# WebSocket Configuration
server.port=8080
spring.websocket.stomp.heartbeat.send-interval=10000
spring.websocket.stomp.heartbeat.receive-interval=10000

# Game Configuration
game.round.duration=90000
game.min.players=2
game.max.players=8
game.points.correct.guess=10
game.points.drawer.bonus=5
```

### Frontend Configuration
```javascript
// WebSocket connection
const WEBSOCKET_URL = 'ws://localhost:8080/ws'

// Game settings
const GAME_CONFIG = {
  roundDuration: 90000,
  canvasWidth: 800,
  canvasHeight: 600,
  brushSizes: [2, 5, 10, 15],
  colors: ['#000', '#ff0000', '#00ff00', '#0000ff']
}
```

## 🎨 Key Components

### GameWebSocketController
- Handles all WebSocket message routing
- Manages game state transitions
- Coordinates between services

### RoomManager
- Room creation and management
- Player joining and leaving
- Word selection and validation
- Score calculation

### GameRoundService
- Round lifecycle management
- Timer integration
- Game state persistence

### TimerService
- Automatic round timing
- Scheduled task management
- Timer synchronization

## 📊 Scoring System

- **Correct Guess**: 10 points
- **Drawer Bonus**: 5 points (when someone guesses correctly)
- **Speed Bonus**: Additional points for quick guesses
- **Streak Bonus**: Bonus points for consecutive correct guesses


## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Nimit** - *Initial work* - [nimit0703](https://github.com/nimit0703)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Vue.js community for the reactive frontend framework
- Contributors and testers

---

**Have fun drawing and guessing! 🎨✨**
