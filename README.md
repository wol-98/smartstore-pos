#SmartStore POS System

A full-stack Enterprise Point of Sale (POS) system built with **Spring Boot** and **MySQL**. This application manages inventory, processes sales, and generates dynamic professional invoices with "Amount in Words" conversion.

## Key Features
* **Dynamic Billing:** Real-time calculation of totals and taxes.
* **Inventory Management:** Automatic stock reduction upon sale.
* **Professional Invoicing:** Generates PDF receipts with company branding and legal footers.
* **Smart Converters:** Automatically converts currency numbers to words (e.g., "450" -> "Four Hundred and Fifty").
* **Reporting:** Export sales data to Excel and email receipts to customers.

## Prerequisites
To run this project, you need:
* **Java JDK 17** or higher.
* **MySQL Server** running on port `3306`.
* **Maven** (Wrapper included in project).

##Setup & Installation

### 1. Database Setup
Before running the app, create the database in MySQL:
```sql
CREATE DATABASE db;
