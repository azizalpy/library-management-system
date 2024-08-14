package com.atmosware.library_project.business.concretes;


import com.atmosware.library_project.business.abstracts.TransactionService;
import com.atmosware.library_project.business.dtos.BookResponse;
import com.atmosware.library_project.business.dtos.TransactionRequest;
import com.atmosware.library_project.business.dtos.TransactionResponse;
import com.atmosware.library_project.business.messages.BusinessMessages;
import com.atmosware.library_project.core.utilities.exceptions.types.BusinessException;
import com.atmosware.library_project.core.utilities.mapping.BookMapper;
import com.atmosware.library_project.core.utilities.mapping.TransactionMapper;
import com.atmosware.library_project.dataAccess.BookRepository;
import com.atmosware.library_project.dataAccess.TransactionRepository;
import com.atmosware.library_project.dataAccess.UserRepository;
import com.atmosware.library_project.entities.Book;
import com.atmosware.library_project.entities.Transaction;
import com.atmosware.library_project.entities.enums.Status;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service("customTransactionManager")
@AllArgsConstructor
public class TransactionManager implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    public TransactionResponse borrowBook(TransactionRequest transactionRequest) {

        checkIfUserExistsByRequest(transactionRequest);

        Transaction transaction = TransactionMapper.INSTANCE.mapToEntity(transactionRequest.getUserId(), transactionRequest);

        List<Book> books = transactionRequest.getBookIds().stream()
                .map(bookId -> bookRepository.findById(bookId)
                        .orElseThrow(() -> new BusinessException(BusinessMessages.BOOK_NOT_FOUND)))
                .map(bookRepository::save)
                .collect(Collectors.toList());

        boolean anyBookBorrowed = books.stream()
                .anyMatch(book -> book.getStatus() == Status.BORROWED);

        if (anyBookBorrowed) {
            throw new BusinessException(BusinessMessages.ALREADY_BORROWED);
        }

        books.forEach(book -> book.setStatus(Status.BORROWED));

        transaction.setBooks(books);
        transaction.setCreatedDate(LocalDateTime.now());
        transaction.setBorrowDate(LocalDateTime.now());
        transaction.setStatus(Status.BORROWED);
        this.transactionRepository.save(transaction);

        return TransactionMapper.INSTANCE.mapToResponse(transaction);
    }

    @Override
    public TransactionResponse returnBook(Long transactionId, List<Long> bookIds) {

        checkIfTransactionExistsById(transactionId);

        Transaction transaction = this.transactionRepository.findById(transactionId).orElse(null);

        List<Book> books = bookIds.stream()
                .map(bookId -> this.bookRepository.findById(bookId)
                        .orElseThrow(() -> new BusinessException(BusinessMessages.BOOK_NOT_FOUND)))
                .collect(Collectors.toList());

        boolean anyBookAlreadyReturned = books.stream()
                .anyMatch(book -> book.getStatus() == Status.RETURNED);

        if (anyBookAlreadyReturned) {
            throw new BusinessException(BusinessMessages.ALREADY_RETURNED);
        }

        books.forEach(book -> book.setStatus(Status.RETURNED));

        // Mevcut kitap listesini güncelle
        List<Book> allBooksInTransaction = transaction.getBooks();

        // Duplicate kontrolü yaparak yeni kitapları ekle  TODO: Daha iyi bir yaklaşımla yapılabilir mi?
        for (Book book : books) {
            if (!allBooksInTransaction.contains(book)) {
                allBooksInTransaction.add(book);
            }
        }

        transaction.setBooks(allBooksInTransaction);

        boolean allBooksReturned = transaction.getBooks().stream()
                .allMatch(book -> book.getStatus() == Status.RETURNED);

        if (allBooksReturned) {
            transaction.setStatus(Status.RETURNED);
        }

        Transaction updatedTransaction = this.transactionRepository.save(transaction);

        return TransactionMapper.INSTANCE.mapToResponse(updatedTransaction);
    }

    @Override
    public List<TransactionResponse> getAll() {

        List<Transaction> transactions = this.transactionRepository.findAll();

        return TransactionMapper.INSTANCE.mapToResponseList(transactions);
    }

    @Override
    public List<BookResponse> getBorrowedBooksByUserId(Long userId) {

        checkIfUserExistsById(userId);

        List<Book> books = this.bookRepository.findBorrowedBooksByUserId(userId, Status.BORROWED);

        return BookMapper.INSTANCE.mapToResponseList(books);
    }

    private void checkIfUserExistsByRequest(TransactionRequest transactionRequest) {

        if(!userRepository.existsById(transactionRequest.getUserId())) {
            throw new BusinessException(BusinessMessages.USER_NOT_FOUND);
        }
    }

    private void checkIfTransactionExistsById(Long transactionId) {

        if(!transactionRepository.existsById(transactionId)) {
            throw new BusinessException(BusinessMessages.TRANSACTION_NOT_FOUND);
        }
    }

    private void checkIfUserExistsById(Long userId) {

        if(!transactionRepository.existsById(userId)) {
            throw new BusinessException(BusinessMessages.USER_NOT_FOUND);
        }
    }

}
