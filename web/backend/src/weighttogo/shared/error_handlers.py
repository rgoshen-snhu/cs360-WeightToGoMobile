"""RFC 7807 Problem Details error handler for FastAPI validation errors.

Replaces FastAPI's default ``{"detail": [...]}`` 422 response with the
RFC 7807 Problem Details shape required by SRS §9.2. Each validation error
is surfaced as ``{field, code, message}`` so the frontend api-client can
map errors directly to form fields.
"""

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse


async def validation_exception_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    """Transform a Pydantic RequestValidationError into an RFC 7807 response.

    Args:
        request: The incoming HTTP request.
        exc: The validation exception raised by FastAPI/Pydantic.

    Returns:
        A 422 JSONResponse with the RFC 7807 Problem Details shape.
    """
    errors = []
    for err in exc.errors():
        loc = list(err.get("loc", []))
        # Strip the leading transport-layer segment (body, query, path, …)
        if loc and loc[0] in {"body", "query", "path", "header", "cookie"}:
            loc = loc[1:]
        errors.append(
            {
                "field": ".".join(str(x) for x in loc) if loc else "",
                "code": str(err.get("type", "value_error")),
                "message": str(err.get("msg", "Invalid value.")),
            }
        )
    body = {
        "type": "about:blank",
        "title": "Validation failed",
        "status": 422,
        "detail": "The submitted data did not pass validation.",
        "instance": str(request.url.path),
        "request_id": request.headers.get("x-request-id", ""),
        "errors": errors,
    }
    return JSONResponse(status_code=422, content=body)
