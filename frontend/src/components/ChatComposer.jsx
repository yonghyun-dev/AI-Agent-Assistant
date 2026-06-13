function ChatComposer({ input, isDisabled, onInputChange, onSubmit }) {
  return (
    <form className="composer" onSubmit={onSubmit}>
      <label className="sr-only" htmlFor="chat-input">
        메시지 입력
      </label>
      <input
        id="chat-input"
        placeholder="메시지를 입력하세요"
        value={input}
        onChange={(event) => onInputChange(event.target.value)}
      />
      <button type="submit" disabled={isDisabled}>
        전송
      </button>
    </form>
  );
}

export default ChatComposer;
