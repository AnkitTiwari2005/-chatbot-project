async function sendMessage() {
    const inputElement = document.getElementById('user-input');
    const message = inputElement.value.trim();
    const chatBox = document.getElementById('chat-box');

    if (!message) return;

    // Display User Message
    appendMessage('You', message, 'user');

    // Clear Input
    inputElement.value = '';

    // Show Typing Indicator
    const typingIndicator = document.createElement('div');
    typingIndicator.className = 'message bot typing';
    typingIndicator.innerHTML = '<span></span><span></span><span></span>';
    chatBox.appendChild(typingIndicator);
    chatBox.scrollTop = chatBox.scrollHeight;

    try {
        const response = await fetch('http://localhost:8080/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message })
        });

        if (!response.ok) throw new Error(`Server Error: ${response.status}`);

        const data = await response.json();

        // Remove Typing Indicator
        chatBox.removeChild(typingIndicator);

        // Display Bot Response
        appendMessage('Bot', data.response, 'bot');
    } catch (error) {
        console.error('Error fetching response:', error);
        chatBox.removeChild(typingIndicator);
        appendMessage('Bot', '❌ Error: Failed to connect to the server.', 'bot');
    }

    chatBox.scrollTop = chatBox.scrollHeight;
}

// Append Message Function
function appendMessage(sender, text, className) {
    const chatBox = document.getElementById('chat-box');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${className}`;
    messageDiv.innerHTML = `<strong>${sender}:</strong> ${text}`;
    chatBox.appendChild(messageDiv);
}

// Handle Enter Key
function handleKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}
