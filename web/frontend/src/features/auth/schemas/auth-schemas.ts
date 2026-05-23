import { z } from 'zod';

const passwordComplexity = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).{12,72}$/;

export const loginSchema = z.object({
  email: z.string().email('Enter a valid email address.'),
  password: z.string().min(1, 'Password is required.'),
});

export const registerSchema = z
  .object({
    email: z.string().email('Enter a valid email address.'),
    password: z
      .string()
      .min(12, 'Password must be at least 12 characters.')
      .max(72, 'Password must be 72 characters or fewer.')
      .regex(
        passwordComplexity,
        'Password must contain an uppercase letter, a lowercase letter, a digit, and a special character.',
      ),
    confirmPassword: z.string(),
    displayName: z
      .string()
      .transform((s) => s.trim())
      .pipe(
        z
          .string()
          .min(2, 'Display name must be at least 2 characters.')
          .max(50, 'Display name must be 50 characters or fewer.'),
      ),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords must match.',
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
