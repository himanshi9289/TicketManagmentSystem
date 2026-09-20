# UI contract: Support Ticket Management

The UI is a Next.js application. It talks to the API in [openapi.yaml](./openapi.yaml). Labels below are what a person sees. Wire values stay the enum names in the API.

## Screens

| Screen | What the person can do |
|--------|------------------------|
| Ticket list | See reference, title, status, priority, assignee or "Unassigned", and last updated time. Newest update first. Search box and status filter. Empty state text when nothing matches, not an error banner. Link to create and to each ticket. |
| Create ticket | Enter title, description, priority, and optional assignee. No status control. On success, open the new ticket. |
| Ticket detail | Show every detail field, created time, updated time, and comments oldest first. Actions: edit details, change status, add comment. Hide or disable edit and comment when status is Closed or Cancelled, and still show the server message if a request is sent anyway. Status control offers only the API values; the server remains the authority. |

Status labels: Open, In Progress, Resolved, Closed, Cancelled.

Priority labels: Low, Medium, High, Critical.

## Errors

Every failed API call returns the five-field object in `ApiError`. The screen shows `message` and does not show `timestamp` as the main text.

- Validation: keep every value already typed. Show `message`. When a segment is `field: reason`, show that reason beside the named field.
- Illegal status change: show `message` unchanged. It already states the current status, the requested status, and that the ticket was not changed. Refresh the detail view from the server so the status on screen matches the stored ticket.
- Finished ticket: show `message` and leave the stored values in place.
- Not found: show `message` and a way back to the list.
- Empty search: do not use the error component. Show that no tickets match.

The form must not clear on HTTP 400. That is how the "values are not dropped" outcome is met, because the error body does not echo the submitted fields.
