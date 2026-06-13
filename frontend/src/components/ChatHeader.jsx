function ChatHeader() {
  return (
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
  );
}

export default ChatHeader;
