import secrets

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBasic, HTTPBasicCredentials

from app.settings import Settings, get_settings


security = HTTPBasic()


def require_basic_auth(
    credentials: HTTPBasicCredentials = Depends(security),
    settings: Settings = Depends(get_settings),
) -> str:
    username_matches = secrets.compare_digest(
        credentials.username,
        settings.app_auth_username,
    )
    password_matches = secrets.compare_digest(
        credentials.password,
        settings.app_auth_password,
    )

    if not (username_matches and password_matches):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication credentials",
            headers={"WWW-Authenticate": "Basic"},
        )

    return credentials.username
