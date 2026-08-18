const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn } = require('child_process');

let mainWindow = null;
let javaProcess = null;

// High DPI & Stable GPU Rendering Switches
app.commandLine.appendSwitch('high-dpi-support', '1');

// Catch any unhandled process exceptions to avoid random desktop exits
process.on('uncaughtException', (err) => {
  console.error('[Desktop] Uncaught exception:', err);
});

function startJavaEngine() {
  try {
    const javaBin = path.join(__dirname, '..', 'engine', 'bin');
    console.log('[Desktop] Spawning Java 26 Voxel Engine on 127.0.0.1:8088...');
    javaProcess = spawn('java', ['-cp', javaBin, 'com.mcjournal.ChunkServer'], {
      cwd: path.join(__dirname, '..'),
      stdio: 'inherit'
    });

    javaProcess.on('error', (err) => {
      console.warn('[Desktop] Java process notice:', err.message);
    });
  } catch (e) {
    console.warn('[Desktop] Could not launch Java engine process:', e);
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 760,
    minWidth: 1024,
    minHeight: 640,
    title: 'Minecraft Journal - Standalone Desktop App',
    backgroundColor: '#111111',
    autoHideMenuBar: true,
    show: false,
    center: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      nodeIntegration: false,
      contextIsolation: true,
      webgl: true
    }
  });

  const distPath = path.join(__dirname, '..', 'dist', 'index.html');
  const fs = require('fs');

  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL);
  } else if (fs.existsSync(distPath)) {
    mainWindow.loadFile(distPath);
  } else {
    mainWindow.loadURL('http://localhost:5173').catch(() => {
      mainWindow.loadFile(distPath);
    });
  }

  mainWindow.once('ready-to-show', () => {
    if (mainWindow) {
      mainWindow.show();
      mainWindow.focus();
    }
  });

  mainWindow.webContents.on('render-process-gone', (event, details) => {
    console.warn('[Desktop] Renderer process gone:', details.reason);
    if (details.reason !== 'clean-exit' && mainWindow) {
      mainWindow.reload();
    }
  });

  // Toggle fullscreen on F11
  mainWindow.webContents.on('before-input-event', (event, input) => {
    if (input.key === 'F11' && input.type === 'keyDown') {
      mainWindow.setFullScreen(!mainWindow.isFullScreen());
      event.preventDefault();
    }
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(() => {
  startJavaEngine();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (javaProcess) {
    try {
      javaProcess.kill();
    } catch (_) {}
  }
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('will-quit', () => {
  if (javaProcess) {
    try {
      javaProcess.kill();
    } catch (_) {}
  }
});
