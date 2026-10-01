import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService } from '../../core/services/api/api.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  selector: 'app-checkout',
  styleUrl: './checkout.css',
  templateUrl: './checkout.html',
})
export class CheckoutComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly apiService = inject(ApiService);

  checkoutType: 'express' | 'quote' = 'express';
  cartItems: any[] = [];
  step = 1;
  isLoading = false;

  customerForm!: FormGroup;
  logisticsForm!: FormGroup;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.checkoutType = params['type'] || 'express';
      this.loadCart();
    });

    this.initForms();
  }

  loadCart() {
    const allCartItems = JSON.parse(localStorage.getItem('smartrent_cart') || '[]');
    this.cartItems = allCartItems.filter((i: any) => i.type === this.checkoutType);

    if (this.cartItems.length === 0) {
      alert('Tu carrito está vacío para este tipo de orden.');
      void this.router.navigate(['/cart']);
    }
  }

  initForms() {
    this.customerForm = this.fb.group({
      customerType: ['PERSON', Validators.required],
      rut: ['', [Validators.required, Validators.pattern(/^\d+-[0-9kK]$/)]],
      name: ['', [Validators.required, Validators.minLength(3)]],
      phone: ['', [Validators.required, Validators.minLength(8)]]
    });

    this.logisticsForm = this.fb.group({
      deliveryType: ['PICKUP', Validators.required], // PICKUP or DISPATCH
      deliveryAddress: [''],
      paymentMethod: ['TRANSFER', Validators.required] // TRANSFER or CARD
    });

    // Make address required if DISPATCH is selected
    this.logisticsForm.get('deliveryType')?.valueChanges.subscribe(type => {
      const addressControl = this.logisticsForm.get('deliveryAddress');
      if (type === 'DISPATCH') {
        addressControl?.setValidators([Validators.required]);
      } else {
        addressControl?.clearValidators();
      }
      addressControl?.updateValueAndValidity();
    });
  }

  get totalItems() {
    return this.cartItems.reduce((acc, item) => acc + (item.machine.dailyPrice || item.machine.pricePerDay), 0);
  }

  nextStep() {
    if (this.customerForm.valid) {
      if (this.checkoutType === 'quote') {
        // Quotes only need step 1
        this.submitOrder();
      } else {
        this.step = 2;
      }
    } else {
      this.customerForm.markAllAsTouched();
    }
  }

  prevStep() {
    this.step = 1;
  }

  submitOrder() {
    if (this.checkoutType === 'express' && this.logisticsForm.invalid) {
      this.logisticsForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;

    const payload = {
      type: this.checkoutType.toUpperCase(),
      ...this.customerForm.value,
      ...this.logisticsForm.value,
      items: this.cartItems.map(item => ({
        machineId: item.machine.id,
        machineName: item.machine.name,
        dailyPrice: item.machine.dailyPrice || item.machine.pricePerDay
      }))
    };

    this.apiService.processCheckout(payload).subscribe({
      next: (res) => {
        this.isLoading = false;
        alert(res.message + ' - Order ID: ' + res.orderId);
        
        // Remove processed items from local cart
        const allCartItems = JSON.parse(localStorage.getItem('smartrent_cart') || '[]');
        const updatedCart = allCartItems.filter((i: any) => i.type !== this.checkoutType);
        localStorage.setItem('smartrent_cart', JSON.stringify(updatedCart));

        void this.router.navigate(['/']);
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
        alert('Hubo un error procesando tu solicitud.');
      }
    });
  }
}
