import { TestBed } from '@angular/core/testing';
import { About } from './about';

describe('About', () => {
  it('should create', async () => {
    await TestBed.configureTestingModule({
      imports: [About]
    }).compileComponents();
    const fixture = TestBed.createComponent(About);
    expect(fixture.componentInstance).toBeTruthy();
  });
});
