class GoogleDriveConnectionManager {
    constructor() {
        this.googleDriveToken = null;
        this.googleDriveFiles = [];
        this.isConnected = false;
    }

    connect() {
        // Logic to handle Google Drive OAuth connection
        // Integrate with existing window.codeClient OAuth flow
        window.codeClient.authenticate().then(token => {
            this.googleDriveToken = token;
            this.isConnected = true;
            this.updateUI();
        }).catch(err => {
            console.error('Connection failed:', err);
        });
    }

    disconnect() {
        // Logic to handle disconnection from Google Drive
        this.googleDriveToken = null;
        this.isConnected = false;
        this.updateUI();
    }

    updateUI() {
        // Update UI elements based on connection state
        const connectButton = document.getElementById('connectButton');
        const disconnectButton = document.getElementById('disconnectButton');
        if (this.isConnected) {
            connectButton.style.display = 'none';
            disconnectButton.style.display = 'block';
        } else {
            connectButton.style.display = 'block';
            disconnectButton.style.display = 'none';
        }
    }

    interceptTokenRequest() {
        // Intercept obtenerTokenBackendYAbrirPicker to detect successful connections
        const originalFunction = obtenerTokenBackendYAbrirPicker;
        window.obtenerTokenBackendYAbrirPicker = (...args) => {
            originalFunction(...args).then(response => {
                if (this.isConnected) {
                    console.log('Successfully connected to Google Drive', response);
                    this.googleDriveFiles = response.files;
                }
                return response;
            });
        };
    }
}