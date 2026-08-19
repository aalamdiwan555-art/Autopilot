package com.autopilot.driver

/**
 * Namespace-safe entry point for authentication.
 *
 * The original authentication implementation is kept in the legacy package
 * because it shares the existing API/session storage. This wrapper gives the
 * manifest a class that is guaranteed to exist under the app namespace.
 */
class LoginActivity : com.mamabhutnika.rideaccepter.LoginActivity()