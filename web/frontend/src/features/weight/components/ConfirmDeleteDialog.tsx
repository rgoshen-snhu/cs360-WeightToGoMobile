/**
 * Confirmation dialog for destructive delete actions.
 *
 * Provides a focus-trapped MUI Dialog with Cancel and Delete buttons.
 * Pressing Escape triggers onCancel (MUI default behaviour).
 */

import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';

interface ConfirmDeleteDialogProps {
  open: boolean;
  /** Called when the user confirms the deletion. */
  onConfirm: () => void;
  /** Called when the user cancels or closes the dialog. */
  onCancel: () => void;
  /** Optional override for the dialog title. */
  title?: string;
  /** Optional override for the body text. */
  description?: string;
}

/**
 * Modal confirmation dialog for weight-entry deletion.
 *
 * Accessible: focus is trapped inside the dialog, the title is linked via
 * aria-labelledby, and the body text is linked via aria-describedby.
 */
export function ConfirmDeleteDialog({
  open,
  onConfirm,
  onCancel,
  title = 'Delete entry',
  description = 'This will permanently delete the weight entry. Are you sure?',
}: ConfirmDeleteDialogProps) {
  return (
    <Dialog
      open={open}
      onClose={onCancel}
      aria-labelledby="confirm-delete-title"
      aria-describedby="confirm-delete-description"
    >
      <DialogTitle id="confirm-delete-title">{title}</DialogTitle>
      <DialogContent>
        <DialogContentText id="confirm-delete-description">{description}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel}>Cancel</Button>
        <Button onClick={onConfirm} color="error" variant="contained">
          Delete
        </Button>
      </DialogActions>
    </Dialog>
  );
}
