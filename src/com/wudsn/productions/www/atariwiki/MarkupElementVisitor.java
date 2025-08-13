package com.wudsn.productions.www.atariwiki;

public interface MarkupElementVisitor {

	void visit(MarkupElement element, int level);
}
