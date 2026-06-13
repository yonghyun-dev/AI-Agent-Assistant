import { useEffect, useRef, useState } from 'react';

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

function createBotReply(message) {
  const trimmed = message.trim();

  if (trimmed.includes('안녕')) {
    return '안녕하세요. 반가워요! 궁금한 내용을 편하게 입력해 주세요.';
  }

  if (trimmed.includes('도움') || trimmed.includes('help')) {
    return '질문을 보내면 제가 간단히 답변하는 챗봇창입니다. 실제 API 연결도 쉽게 붙일 수 있어요.';
  }

  return `"${trimmed}"에 대해 조금 더 알려주시면 이어서 도와드릴게요.`;
}

function App() {
  const [messages, setMessages] = useState(INITIAL_MESSAGES);
  const [input, setInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messageEndRef = useRef(null);

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  function handleSubmit(event) {
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
    setIsTyping(true);

    window.setTimeout(() => {
      const botMessage = {
        id: crypto.randomUUID(),
        role: 'bot',
        text: createBotReply(text),
        time: getCurrentTime(),
      };

      setMessages((currentMessages) => [...currentMessages, botMessage]);
      setIsTyping(false);
    }, 650);
  }

  return (
    <main className="app-shell">
      <section className="chat-window" aria-label="챗봇">
        <header className="chat-header">
          <div>
            <p className="eyebrow">React Chatbot</p>
            <h1>AI Assistant</h1>
          </div>
          <span className="status">
            <span aria-hidden="true" />
            온라인
          </span>
        </header>

        <div className="message-list" role="log" aria-live="polite">
          {messages.map((message) => (
            <article
              className={`message-row ${message.role === 'user' ? 'is-user' : ''}`}
              key={message.id}
            >
              <div className="message-bubble">
                <p>{message.text}</p>
                <time>{message.time}</time>
              </div>
            </article>
          ))}

          {isTyping && (
            <article className="message-row">
              <div className="message-bubble typing" aria-label="답변 작성 중">
                <span />
                <span />
                <span />
              </div>
            </article>
          )}
          <div ref={messageEndRef} />
        </div>

        <form className="composer" onSubmit={handleSubmit}>
          <label className="sr-only" htmlFor="chat-input">
            메시지 입력
          </label>
          <input
            id="chat-input"
            placeholder="메시지를 입력하세요"
            value={input}
            onChange={(event) => setInput(event.target.value)}
          />
          <button type="submit" disabled={!input.trim() || isTyping}>
            전송
          </button>
        </form>
      </section>
    </main>
  );
}

export default App;
