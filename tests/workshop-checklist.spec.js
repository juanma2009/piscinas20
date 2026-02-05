import { test, expect } from '@playwright/test';

test('Verify checklist buttons', async ({ page }) => {
  // Go to orders list
  await page.goto('http://localhost:8080/pedidos/listarPedidos');
  
  // Wait for DataTable to load
  await page.waitForSelector('table#table tbody tr');
  
  // Check if "Checklist Taller" button exists in the first row
  const checklistButton = page.locator('table#table tbody tr:first-child .btn-outline-dark[title="Checklist Taller"]');
  await expect(checklistButton).toBeVisible();
  
  // Click the button
  await checklistButton.click();
  
  // Verify we are on the checklist page
  await expect(page).toHaveURL(/\/workshop\/checklist\/\d+/);
  await expect(page.locator('h1, .card-header')).toContainText(/Checklist/i);
  
  // Go back to order details
  const orderId = page.url().split('/').pop();
  await page.goto(`http://localhost:8080/pedidos/ver/${orderId}`);
  
  // Check if "Checklist" button exists in the toolbar
  const detailChecklistButton = page.locator('.toolbar a[href*="/workshop/checklist/"]');
  await expect(detailChecklistButton).toBeVisible();
  await expect(detailChecklistButton).toContainText('Checklist');
  
  // Click it
  await detailChecklistButton.click();
  
  // Verify again
  await expect(page).toHaveURL(new RegExp(`/workshop/checklist/${orderId}`));
});
