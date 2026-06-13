import MessageBubble from './MessageBubble.jsx';
import TypingIndicator from './TypingIndicator.jsx';

function MessageList({ error, isTyping, messages, messageEndRef }) {
  return (
    <div className="message-list" role="log" aria-live="polite">
      {messages.map((message) => (
        <MessageBubble key={message.id} message={message} />
      ))}

      {error && (
        <article className="message-row">
          <div className="message-bubble error-message">
            <p>{error}</p>
          </div>
        </article>
      )}

      {isTyping && <TypingIndicator />}
      <div ref={messageEndRef} />
    </div>
  );
}

export default MessageList;
