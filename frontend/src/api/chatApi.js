const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
const AUTH_USERNAME = import.meta.env.VITE_AUTH_USERNAME || 'admin';
const AUTH_PASSWORD = import.meta.env.VITE_AUTH_PASSWORD || 'admin1234';

function createAuthHeader() {
  // 브라우저는 Spring 게이트웨이에만 인증 정보를 보냅니다.
  // Spring에서 인증을 통과한 요청만 내부 FastAPI 백엔드로 전달됩니다.
  return `Basic ${btoa(`${AUTH_USERNAME}:${AUTH_PASSWORD}`)}`;
}

export async function sendChatMessage(message) {
  const response = await fetch(`${API_BASE_URL}/chat`, {
    method: 'POST',
    headers: {
      Authorization: createAuthHeader(),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ message }),
  });

  if (!response.ok) {
    throw new Error(`Chat API request failed: ${response.status}`);
  }

  const data = await response.json();
  return data.reply;
}
