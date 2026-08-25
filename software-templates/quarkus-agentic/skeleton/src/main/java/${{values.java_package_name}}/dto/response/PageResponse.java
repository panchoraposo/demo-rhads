package ${{values.java_package_name}}.dto.response;

import java.util.List;

public class PageResponse<T> {
    
    public List<T> content;
    public int currentPage;
    public int totalPages;
    public long totalElements;
    public int numberOfElements;
    public int size;
    public boolean first;
    public boolean last;
    public boolean empty;
    
    public PageResponse() {
    }
    
    public PageResponse(List<T> content, int currentPage, int totalPages, long totalElements, int size) {
        this.content = content;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.numberOfElements = content != null ? content.size() : 0;
        this.size = size;
        this.first = currentPage == 0;
        this.last = currentPage >= totalPages - 1;
        this.empty = numberOfElements == 0;
    }
}