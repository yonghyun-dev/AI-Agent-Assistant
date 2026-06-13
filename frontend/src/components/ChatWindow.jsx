import ChatComposer from './ChatComposer.jsx';
import ChatHeader from './ChatHeader.jsx';
import MessageList from './MessageList.jsx';

function ChatWindow({
  error,
  input,
  isTyping,
  messages,
  messageEndRef,
  onInputChange,
  onSubmit,
}) {
  return (
    <section className="chat-window" aria-label="챗봇">
      <ChatHeader />
      <MessageList
        error={error}
        isTyping={isTyping}
        messages={messages}
        messageEndRef={messageEndRef}
      />
      <ChatComposer
        input={input}
        isDisabled={!input.trim() || isTyping}
        onInputChange={onInputChange}
        onSubmit={onSubmit}
      />
    </section>
  );
}

export default ChatWindow;
