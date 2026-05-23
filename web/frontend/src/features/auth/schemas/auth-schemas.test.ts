import { describe, expect, it } from 'vitest';
import { loginSchema, registerSchema } from './auth-schemas';

describe('loginSchema', () => {
  it('accepts valid email and non-empty password', () => {
    const result = loginSchema.safeParse({ email: 'a@b.co', password: 'x' });
    expect(result.success).toBe(true);
  });

  it('rejects malformed email', () => {
    const result = loginSchema.safeParse({ email: 'not-an-email', password: 'x' });
    expect(result.success).toBe(false);
  });

  it('rejects empty password', () => {
    const result = loginSchema.safeParse({ email: 'a@b.co', password: '' });
    expect(result.success).toBe(false);
  });
});

describe('registerSchema', () => {
  const valid = {
    email: 'jane@example.com',
    password: 'Aa1!aaaaaaaa',
    confirmPassword: 'Aa1!aaaaaaaa',
    displayName: 'Jane Doe',
  };

  it('accepts a valid registration', () => {
    expect(registerSchema.safeParse(valid).success).toBe(true);
  });

  it('rejects password shorter than 12 chars', () => {
    const result = registerSchema.safeParse({
      ...valid,
      password: 'Aa1!aaaa',
      confirmPassword: 'Aa1!aaaa',
    });
    expect(result.success).toBe(false);
  });

  it('rejects password missing uppercase', () => {
    const result = registerSchema.safeParse({
      ...valid,
      password: 'aa1!aaaaaaaa',
      confirmPassword: 'aa1!aaaaaaaa',
    });
    expect(result.success).toBe(false);
  });

  it('rejects password missing symbol', () => {
    const result = registerSchema.safeParse({
      ...valid,
      password: 'Aa1aaaaaaaaa',
      confirmPassword: 'Aa1aaaaaaaaa',
    });
    expect(result.success).toBe(false);
  });

  it('rejects display name shorter than 2 characters after trim', () => {
    expect(registerSchema.safeParse({ ...valid, displayName: ' a ' }).success).toBe(false);
  });

  it('rejects display name longer than 50 characters', () => {
    expect(registerSchema.safeParse({ ...valid, displayName: 'a'.repeat(51) }).success).toBe(false);
  });

  it('rejects mismatched confirmPassword', () => {
    expect(registerSchema.safeParse({ ...valid, confirmPassword: 'different' }).success).toBe(
      false,
    );
  });
});
