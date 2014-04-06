package edu.sjsu.cmpe.library.api.resources;

import java.net.URL;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sjsu.edu.library.stompMessage.StompMessaging;
import com.yammer.dropwizard.jersey.params.LongParam;
import com.yammer.metrics.annotation.Timed;

import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.dto.BookDto;
import edu.sjsu.cmpe.library.dto.BooksDto;
import edu.sjsu.cmpe.library.dto.LinkDto;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

@Path("/v1/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {
    /** bookRepository instance */
    private final BookRepositoryInterface bookRepository;
    private final StompMessaging stompInstance;

    /**
     * BookResource constructor
     * 
     * @param bookRepository
     *            a BookRepository instance
     */
    public BookResource(BookRepositoryInterface bookRepository,StompMessaging instance) {
	this.bookRepository = bookRepository;
	this.stompInstance = instance;
    }

    @GET
    @Path("/{isbn}")
    @Timed(name = "view-book")
    public BookDto getBookByIsbn(@PathParam("isbn") LongParam isbn) {
	Book book = bookRepository.getBookByISBN(isbn.get());
	BookDto bookResponse = new BookDto(book);
	bookResponse.addLink(new LinkDto("view-book", "/books/" + book.getIsbn(),
		"GET"));
	bookResponse.addLink(new LinkDto("update-book-status", "/books/"
		+ book.getIsbn(), "PUT"));
	// add more links

	return bookResponse;
    }

    @POST
    @Timed(name = "create-book")
    public Response createBook(@Valid Book request) {
	// Store the new book in the BookRepository so that we can retrieve it.
	Book savedBook = bookRepository.saveBook(request,(long) 0);

	String location = "/books/" + savedBook.getIsbn();
	BookDto bookResponse = new BookDto(savedBook);
	bookResponse.addLink(new LinkDto("view-book", location, "GET"));
	bookResponse
	.addLink(new LinkDto("update-book-status", location, "PUT"));

	return Response.status(201).entity(bookResponse).build();
    }

    @GET
    @Path("/")
    @Timed(name = "view-all-books")
    public BooksDto getAllBooks() {
	BooksDto booksResponse = new BooksDto(bookRepository.getAllBooks());
	booksResponse.addLink(new LinkDto("create-book", "/books", "POST"));

	return booksResponse;
    }

    @PUT
    @Path("/{isbn}")
    @Timed(name = "update-book-status")
    public Response updateBookStatus(@PathParam("isbn") LongParam isbn,
	    @DefaultValue("available") @QueryParam("status") Status status) {
	Book book = bookRepository.getBookByISBN(isbn.get());
	book.setStatus(status);
	if(status.getValue() == "lost") 
	{
		Connection connect;
		
		    try
		    {
				connect = stompInstance.createConnection();
				stompInstance.sendMsgQueue(connect,book.getIsbn());
				connect.close();
		    }
		    catch(JMSException e)
		    {
		    	e.printStackTrace();
		    }
		    catch(Exception e)
		    {
		    	e.printStackTrace();
		    }
		   
	}

	BookDto bookResponse = new BookDto(book);
	String location = "/books/" + book.getIsbn();
	bookResponse.addLink(new LinkDto("view-book", location, "GET"));

	return Response.status(200).entity(bookResponse).build();
    }
    
    /*
     * update Library API
     */
    @POST
    @Path("/update")
    @Timed(name = "update-library")
    public Response updateLibrary(@Valid String msg) throws Exception {
    	
    	String[] updateMessage=msg.split(":", 4); 
        Long isbn = Long.valueOf(updateMessage[0]);
        Status status = Status.available;

        Book book = bookRepository.getBookByISBN(isbn);
        
        /**If book received from Publisher is equal to lost book, update status*/
        if (book != null && book.getStatus()==Status.lost) {
        	book.setStatus(status);
        	System.out.println("Book " + book.getIsbn() +" updated to 'available' status");
        }
        
        /**If book received from Publisher is new book, add to hashmap*/
        else if (book == null){
        	String title = updateMessage[1];
        	String category = updateMessage[2];
        	URL coverImage = new URL(updateMessage[3]);
        	Book newBook = new Book();
        	//newBook.setIsbn(isbn);
        	newBook.setTitle(title);
        	newBook.setCategory(category);
        	newBook.setCoverimage(coverImage);
        	bookRepository.saveBook(newBook,isbn);
        	System.out.println("Book " + newBook.getIsbn() + " added to library");
        }
        
        // already in library
        else {
        	System.out.println("Book " + book.getIsbn() + " already in Library ");
        }

        return Response.ok().build();
    }

    @DELETE
    @Path("/{isbn}")
    @Timed(name = "delete-book")
    public BookDto deleteBook(@PathParam("isbn") LongParam isbn) {
	bookRepository.delete(isbn.get());
	BookDto bookResponse = new BookDto(null);
	bookResponse.addLink(new LinkDto("create-book", "/books", "POST"));

	return bookResponse;
    }
}

