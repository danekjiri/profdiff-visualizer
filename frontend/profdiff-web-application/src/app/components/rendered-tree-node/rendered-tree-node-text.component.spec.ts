import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RenderedTreeNodeTextComponent } from './rendered-tree-node-text.component';
import { RenderedTreeNode } from '../../models/dto/renderedTreeNode';
import { RenderedTreeNodeMarker } from '../../models/dto/renderedTreeNodeMarker';

describe('RenderedTreeNodeTextComponent', () => {
  let component: RenderedTreeNodeTextComponent;
  let fixture: ComponentFixture<RenderedTreeNodeTextComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RenderedTreeNodeTextComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(RenderedTreeNodeTextComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.componentRef.setInput('node', {
      content: { rawText: 'Root Node' },
      marker: RenderedTreeNodeMarker.Neutral,
      children: []
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('should display the correct raw text', () => {
    fixture.componentRef.setInput('node', {
      content: { rawText: 'root' },
      marker: RenderedTreeNodeMarker.Neutral,
      children: []
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    const textSpan = fixture.debugElement.query(By.css('.node-content .text'));
    expect(textSpan.nativeElement.textContent.trim()).toBe('root');
  });

  it('should map INSERT marker to "+" symbol and "insert" class', () => {
    fixture.componentRef.setInput('node', {
      content: { rawText: 'inserted node' },
      marker: RenderedTreeNodeMarker.Insert,
      children: []
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    const contentDiv = fixture.debugElement.query(By.css('.node-content'));
    const markerCol = fixture.debugElement.query(By.css('.marker-col'));

    expect(contentDiv.classes['insert']).toBe(true);
    expect(markerCol.nativeElement.textContent.trim()).toBe('+');
  });

  it('should map DELETE marker to "-" symbol and "delete" class', () => {
    fixture.componentRef.setInput('node', {
      content: { rawText: 'deleted node' },
      marker: RenderedTreeNodeMarker.Delete,
      children: []
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    const contentDiv = fixture.debugElement.query(By.css('.node-content'));
    const markerCol = fixture.debugElement.query(By.css('.marker-col'));

    expect(contentDiv.classes['delete']).toBe(true);
    expect(markerCol.nativeElement.textContent.trim()).toBe('-');
  });

  it('should map RELABEL marker to "*" symbol, "relabel" class, and render right side', () => {
    fixture.componentRef.setInput('node', {
      content: { rawText: 'relabel node left' },
      marker: RenderedTreeNodeMarker.Relabel,
      children: []
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    const contentDiv = fixture.debugElement.query(By.css('.node-content'));
    const markerCol = fixture.debugElement.query(By.css('.marker-col'));
    const textSpans = fixture.debugElement.queryAll(By.css('.node-content .text'));

    expect(contentDiv.classes['relabel']).toBe(true);
    expect(markerCol.nativeElement.textContent.trim()).toBe('*');
    expect(textSpans[0].nativeElement.textContent.trim()).toBe('relabel node left');
  });

  it('should use a "." symbol and default lowercase class for IDENTITY', () => {
    fixture.componentRef.setInput('node', {
      content: { rawText: 'identity node' },
      marker: RenderedTreeNodeMarker.Identity,
      children: []
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    const contentDiv = fixture.debugElement.query(By.css('.node-content'));
    const markerCol = fixture.debugElement.query(By.css('.marker-col'));

    expect(contentDiv.classes['identity']).toBe(true);
    expect(markerCol.nativeElement.textContent.trim()).toBe('.');
  });

  it('should recursively render regular children components', () => {
    const mockTree = {
      content: { rawText: 'root' },
      marker: RenderedTreeNodeMarker.Identity,
      children: [
        {
          content: { rawText: 'child1' },
          marker: RenderedTreeNodeMarker.Insert,
          children: []
        },
        {
          content: { rawText: 'child2' },
          marker: RenderedTreeNodeMarker.Identity,
          children: [
            {
              content: { rawText: 'grandchild1' },
              marker: RenderedTreeNodeMarker.Delete,
              children: []
            }
          ]
        }
      ]
    } as unknown as RenderedTreeNode;

    fixture.componentRef.setInput('node', mockTree);
    fixture.detectChanges();

    const childComponents = fixture.debugElement.queryAll(By.directive(RenderedTreeNodeTextComponent));
    expect(childComponents.length).toBe(3);
    
    const rawDomText = fixture.nativeElement.textContent;
    expect(rawDomText).toContain('root');
    expect(rawDomText).toContain('child1');
    expect(rawDomText).toContain('child2');
    expect(rawDomText).toContain('grandchild1');
  });
    
  it('should toggle tree node expansion when clicking the node content', () => {
    fixture.componentRef.setInput('node', {
      content: { rawText: 'root with children' },
      marker: RenderedTreeNodeMarker.Identity,
      children: [{ content: { rawText: 'child node' }, marker: RenderedTreeNodeMarker.Identity, children: [] }]
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    const mainNodeContent = fixture.debugElement.query(By.css('.node-content'));
    
    let childrenContainer = fixture.debugElement.query(By.css('.children-container'));
    expect(childrenContainer).toBeTruthy();

    mainNodeContent.nativeElement.click();
    fixture.detectChanges();

    childrenContainer = fixture.debugElement.query(By.css('.children-container'));
    expect(childrenContainer).toBeFalsy();

    mainNodeContent.nativeElement.click();
    fixture.detectChanges();

    childrenContainer = fixture.debugElement.query(By.css('.children-container'));
    expect(childrenContainer).toBeTruthy();
  });

  it('should toggle INFO details expansion when clicking the info header', () => {
    fixture.componentRef.setInput('node', {
      content: { rawText: 'root' },
      marker: RenderedTreeNodeMarker.Identity,
      children: [
        { content: { rawText: '▶ RECEIVER-TYPE-PROFILER || REASONING' }, marker: 'INFO', children: [] },
        { content: { rawText: '90% HashMap' }, marker: 'INFO', children: [] },
        { content: { rawText: '10% Arrays' }, marker: 'INFO', children: [] }
      ]
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    const infoHeader = fixture.debugElement.query(By.css('.node-content.info'));
    expect(infoHeader).toBeTruthy();
    expect(infoHeader.nativeElement.textContent).toContain('▶ RECEIVER-TYPE-PROFILER || REASONING');

    let infoDetails = fixture.debugElement.queryAll(By.css('.node-content.info'));
    expect(infoDetails.length).toBe(1);

    infoHeader.nativeElement.click();
    fixture.detectChanges();

    infoDetails = fixture.debugElement.queryAll(By.css('.node-content.info'));
    expect(infoDetails.length).toBe(4); // 3 plus wrapper
    
    infoHeader.nativeElement.click();
    fixture.detectChanges();

    infoDetails = fixture.debugElement.queryAll(By.css('.node-content.info'));
    expect(infoDetails.length).toBe(1);
  });

  it('should emit methodClicked event when a method link is clicked', () => {
    jest.spyOn(component.methodClicked, 'emit');
    fixture.componentRef.setInput('node', {
      content: { methodName: 'java.lang.String::length' },
      marker: RenderedTreeNodeMarker.Identity,
      children: []
    } as unknown as RenderedTreeNode);
    fixture.detectChanges();

    const methodLink = fixture.debugElement.query(By.css('.method-link'));
    methodLink.nativeElement.click();

    expect(component.methodClicked.emit).toHaveBeenCalledWith('java.lang.String::length');
  });
});