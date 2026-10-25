package com.jibanez.paymentcore.exception;

public final class BusinessExceptions {

    private BusinessExceptions() {
    }

    public static AccountNotFoundException accountNotFound() {
        return new AccountNotFoundException();
    }

    public static final class AccountNotFoundException extends BusinessException {
        public AccountNotFoundException() {
            super("ACCOUNT_NOT_FOUND", "Cuenta no encontrada", 404);
        }
    }

    public static final class InsufficientBalanceException extends BusinessException {
        public InsufficientBalanceException() {
            super("INSUFFICIENT_BALANCE", "Saldo insuficiente para la operacion", 409);
        }
    }

    public static final class AccountInactiveException extends BusinessException {
        public AccountInactiveException() {
            super("ACCOUNT_INACTIVE", "La cuenta no esta activa", 409);
        }
    }

    public static final class SameAccountException extends BusinessException {
        public SameAccountException() {
            super("SAME_ACCOUNT", "La transferencia debe ir a una cuenta distinta", 422);
        }
    }

    public static final class InvalidAmountException extends BusinessException {
        public InvalidAmountException() {
            super("INVALID_AMOUNT", "El monto debe ser mayor a cero", 422);
        }
    }

    public static final class AccountAccessDeniedException extends BusinessException {
        public AccountAccessDeniedException() {
            super("ACCOUNT_ACCESS_DENIED", "No tienes acceso a esa cuenta", 403);
        }
    }

    public static final class UsernameTakenException extends BusinessException {
        public UsernameTakenException() {
            super("USERNAME_TAKEN", "El nombre de usuario ya existe", 409);
        }
    }

    public static final class InvalidCredentialsException extends BusinessException {
        public InvalidCredentialsException() {
            super("INVALID_CREDENTIALS", "Credenciales invalidas", 401);
        }
    }

    public static final class IdempotencyConflictException extends BusinessException {
        public IdempotencyConflictException() {
            super("IDEMPOTENCY_CONFLICT", "La clave de idempotencia ya fue usada con un cuerpo distinto", 409);
        }
    }
}