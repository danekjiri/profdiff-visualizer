import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MethodSelectorComponent, MethodSelectItem } from './method-selector.component';
import { By } from '@angular/platform-browser';

describe('MethodSelectorComponent', () => {
  let component: MethodSelectorComponent<string>;
  let fixture: ComponentFixture<MethodSelectorComponent<string>>;

  const mockItems: MethodSelectItem<string>[] = [
    { name: 'java.util.List.add', value: "ListPutMethod", hotness: 0.8 },
    { name: 'java.util.Map.put', value: 'MapPutMethod', isUnpaired: true },
    { name: 'org.example.Main.run', value: 'MainRunMethod' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MethodSelectorComponent,
        NoopAnimationsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent<MethodSelectorComponent<string>>(MethodSelectorComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('items', mockItems);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should filter items based on search query', () => {
    const inputEvent = { target: { value: 'Map' } } as unknown as Event;
    component['onSearchInput'](inputEvent);
    fixture.detectChanges();

    expect(component['filteredItems']().length).toBe(1);
    expect(component['filteredItems']()[0].name).toContain('Map');
  });

  it('should emit selectionChange when an item is selected', () => {
    const spy = jest.fn();
    component.selectionChange.subscribe(spy);

    component['onSelectionChange']('java.util.Map.put');
    
    expect(spy).toHaveBeenCalledWith('MapPutMethod');
  });

  it('should clear search query and reset scroll on clearSearch', () => {
    component['searchQuery'].set('test');
    fixture.detectChanges();

    const stopPropagationSpy = jest.fn();
    component['clearSearch']({ stopPropagation: stopPropagationSpy } as any);

    expect(component['searchQuery']()).toBe('');
    expect(stopPropagationSpy).toHaveBeenCalled();
  });

  it('should reset search query when dropdown is closed', () => {
    component['searchQuery'].set('searching...');
    
    component['onOpenedChange'](false);
    
    expect(component['searchQuery']()).toBe('');
  });
  
  it('should handle empty results message', () => {
    component.updateSearch('NonExistentMethod');
    fixture.detectChanges();

    const noResultsEl = fixture.debugElement.query(By.css('.test-no-results-mirror'));
    
    expect(component['filteredItems']().length).toBe(0);
    expect(noResultsEl).toBeTruthy();
    expect(noResultsEl.nativeElement.textContent).toContain('No methods match "NonExistentMethod"');
  });

  it('should show hotness and unpaired chips correctly', () => {
    const hotItem = component['filteredItems']().find(i => i.name === 'java.util.List.add');
    const unpairedItem = component['filteredItems']().find(i => i.name === 'java.util.Map.put');

    expect(hotItem?.hotness).toBeGreaterThan(0);
    expect(unpairedItem?.isUnpaired).toBe(true);
  });

  it('should scroll to the selected item when opened', fakeAsync(() => {
    fixture.componentRef.setInput('selectedItem', mockItems[2]);
    fixture.detectChanges();

    const scrollSpy = jest.fn(); 
    component['viewport'] = { 
      scrollToIndex: scrollSpy,
      checkViewportSize: jest.fn()
    } as any;

    component['onOpenedChange'](true);
    tick();

    expect(scrollSpy).toHaveBeenCalledWith(2);
  }));
});