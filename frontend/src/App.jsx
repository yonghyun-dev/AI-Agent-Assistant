import { useEffect, useRef, useState } from 'react';
import { sendChatMessage } from './api/chatApi.js';
import ChatWindow from './components/ChatWindow.jsx';

const INITIAL_MESSAGES = [
  {
    id: 1,
    role: 'bot',
    text: '안녕하세요! 무엇을 도와드릴까요?',
    time: '09:30',
  },
];

function getCurrentTime() {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date());
}

function App() {
  const [messages, setMessages] = useState(INITIAL_MESSAGES);
  const [input, setInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [error, setError] = useState('');
  const messageEndRef = useRef(null);

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, isTyping, error]);

  async function handleSubmit(event) {
    event.preventDefault();

    const text = input.trim();
    if (!text || isTyping) {
      return;
    }

    const userMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      text,
      time: getCurrentTime(),
    };

    setMessages((currentMessages) => [...currentMessages, userMessage]);
    setInput('');
    setError('');
    setIsTyping(true);

    try {
      const reply = await sendChatMessage(text);
      const botMessage = {
        id: crypto.randomUUID(),
        role: 'bot',
        text: reply || '응답이 비어 있습니다.',
        time: getCurrentTime(),
      };

      setMessages((currentMessages) => [...currentMessages, botMessage]);
    } catch (apiError) {
      console.error(apiError);
      setError('서버 응답을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setIsTyping(false);
    }
  }

  return (
    <main className="app-shell">
      <ChatWindow
        error={error}
        input={input}
        isTyping={isTyping}
        messages={messages}
        messageEndRef={messageEndRef}
        onInputChange={setInput}
        onSubmit={handleSubmit}
      />
    </main>
  );
}

export default App;
