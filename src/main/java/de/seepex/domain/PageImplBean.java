package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PageImplBean<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private int number;
    private int size;
    private int totalPages;
    private int numberOfElements;
    private long total;
    private boolean previousPage;
    private boolean first;
    private boolean nextPage;
    private boolean last;
    private List<T> content;
    private PageResult pageable;
    private Float maxScore;
    private int totalElements;
    
    public PageImplBean() {
        new ArrayList<T>();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public boolean isPreviousPage() {
        return previousPage;
    }

    public void setPreviousPage(boolean previousPage) {
        this.previousPage = previousPage;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isNextPage() {
        return nextPage;
    }

    public void setNextPage(boolean nextPage) {
        this.nextPage = nextPage;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public PageResult getPageable() {
        return pageable;
    }

    public void setPageable(PageResult pageable) {
        this.pageable = pageable;
    }

    public Float getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Float maxScore) {
        this.maxScore = maxScore;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public PageImpl<T> pageImpl() {
        PageRequest pageRequest;
        if(pageable == null) {
            pageRequest = PageRequest.of(0, 20);
        } else {
            pageRequest = PageRequest.of(pageable.getPage(), pageable.getSize());
        }

        return new PageImpl<T>(getContent(), pageRequest , totalElements);
    }
}
