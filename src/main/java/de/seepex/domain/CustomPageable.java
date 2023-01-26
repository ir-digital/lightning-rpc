package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomPageable {

    private Integer pageSize;
    private Integer pageNumber;
    private long offset;
    private Boolean paged;
    private Boolean unpaged;
    private CustomSort sort;

    public static CustomPageable of(PageRequest pageRequest) {

        CustomPageable customPageable = new CustomPageable();
        customPageable.setPageSize(pageRequest.getPageSize());
        customPageable.setPageNumber(pageRequest.getPageNumber());
        customPageable.setOffset(pageRequest.getOffset());
        customPageable.setPaged(pageRequest.isPaged());
        customPageable.setUnpaged(pageRequest.isUnpaged());

        Sort sort = pageRequest.getSort();

        if(sort.isSorted()) {
            CustomSort customSort = new CustomSort();
            for (Sort.Order next : sort) {
                CustomSortOrder sortOrder = new CustomSortOrder();
                sortOrder.setProperty(next.getProperty());
                sortOrder.setDirection(next.getDirection().name());
                
                customSort.getSortOrders().add(sortOrder);
            }

            customPageable.setSort(customSort);
        }

        return customPageable;
    }

    public CustomSort getSort() {
        return sort;
    }

    public void setSort(CustomSort sort) {
        this.sort = sort;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public Boolean getPaged() {
        return paged;
    }

    public void setPaged(Boolean paged) {
        this.paged = paged;
    }

    public Boolean getUnpaged() {
        return unpaged;
    }

    public void setUnpaged(Boolean unpaged) {
        this.unpaged = unpaged;
    }
}
