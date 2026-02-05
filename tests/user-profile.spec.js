const { test, expect } = require('@playwright/test');

test.describe('User Profile Modification', () => {
  test('should display and update user profile', async ({ page }) => {
    // Login as user
    await page.goto('http://localhost:8080/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Navigate to profile
    await page.goto('http://localhost:8080/user/profile');

    // Verify profile form is displayed
    await expect(page.locator('h1')).toContainText('Configuración del Perfil');

    // Update profile information
    await page.fill('input[name="username"]', 'updateduser');
    await page.fill('input[name="password"]', 'newpassword');
    await page.fill('input[name="email"]', 'updated@example.com');
    await page.fill('input[name="nombre"]', 'Updated Name');
    await page.fill('input[name="apellido"]', 'Updated Lastname');
    await page.click('button:has-text("Actualizar Información")');

    // Verify success message
    await expect(page.locator('text=Perfil actualizado correctamente')).toBeVisible();

    // Verify updated values
    await expect(page.locator('input[name="username"]')).toHaveValue('updateduser');
    await expect(page.locator('input[name="email"]')).toHaveValue('updated@example.com');
    await expect(page.locator('input[name="nombre"]')).toHaveValue('Updated Name');
    await expect(page.locator('input[name="apellido"]')).toHaveValue('Updated Lastname');
  });

  test('should change password successfully', async ({ page }) => {
    // Login
    await page.goto('http://localhost:8080/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Go to profile
    await page.goto('http://localhost:8080/user/profile');

    // Change password
    await page.fill('input[name="currentPassword"]', 'password');
    await page.fill('input[name="newPassword"]', 'newpassword123');
    await page.fill('input[name="confirmPassword"]', 'newpassword123');
    await page.click('button:has-text("Cambiar Contraseña")');

    // Verify success
    await expect(page.locator('text=Contraseña cambiada correctamente')).toBeVisible();
  });

  test('should toggle 2FA', async ({ page }) => {
    // Login
    await page.goto('http://localhost:8080/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Go to profile
    await page.goto('http://localhost:8080/user/profile');

    // Toggle 2FA
    await page.check('input[name="enable2FA"]');
    await page.click('button:has-text("Actualizar 2FA")');

    // Verify success
    await expect(page.locator('text=Configuración 2FA actualizada')).toBeVisible();
  });

  test('should show error for incorrect current password', async ({ page }) => {
    // Login
    await page.goto('http://localhost:8080/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Go to profile
    await page.goto('http://localhost:8080/user/profile');

    // Attempt password change with wrong current password
    await page.fill('input[name="currentPassword"]', 'wrongpassword');
    await page.fill('input[name="newPassword"]', 'newpassword123');
    await page.fill('input[name="confirmPassword"]', 'newpassword123');
    await page.click('button:has-text("Cambiar Contraseña")');

    // Verify error
    await expect(page.locator('text=La contraseña actual es incorrecta')).toBeVisible();
  });

  test('should show error for password too short', async ({ page }) => {
    // Login
    await page.goto('http://localhost:8080/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Go to profile
    await page.goto('http://localhost:8080/user/profile');

    // Attempt password change with short new password
    await page.fill('input[name="currentPassword"]', 'password');
    await page.fill('input[name="newPassword"]', '123');
    await page.fill('input[name="confirmPassword"]', '123');
    await page.click('button:has-text("Cambiar Contraseña")');

    // Verify error
    await expect(page.locator('text=La nueva contraseña debe tener al menos 6 caracteres')).toBeVisible();
  });
});