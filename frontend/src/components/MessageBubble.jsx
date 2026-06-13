function MessageBubble({ message }) {
  return (
    <article className={`message-row ${message.role === 'user' ? 'is-user' : ''}`}>
      <div className="message-bubble">
        <p>{message.text}</p>
        <time>{message.time}</time>
      </div>
    </article>
  );
}

export default MessageBubble;
