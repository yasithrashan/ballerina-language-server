import ballerina/grpc;
import ballerina/time;

listener grpc:Listener productListener = new (9092);

@grpc:Descriptor {value: PRODUCT_DESC}
service "ProductService" on productListener {

    private map<ProductResponse> productStore = {};

    remote function GetProduct(ProductRequest productRequest) returns ProductResponse|error {
        string productId = productRequest.product_id;
        ProductResponse? productResponse = self.productStore[productId];
        if productResponse is ProductResponse {
            return productResponse;
        }
        return error("Product not found with ID: " + productId);
    }

    remote function CreateProduct(CreateProductRequest createRequest) returns ProductResponse|error {
        string productId = "PROD_" + self.productStore.length().toString();
        time:Utc currentTime = time:utcNow();
        string createdAt = time:utcToString(currentTime);

        ProductResponse newProduct = {
            product_id: productId,
            name: createRequest.name,
            description: createRequest.description,
            price: createRequest.price,
            created_at: createdAt
        };

        self.productStore[productId] = newProduct;
        return newProduct;
    }

    remote function ListProducts(Empty emptyRequest) returns ProductListResponse|error {
        ProductResponse[] productList = self.productStore.toArray();
        return {products: productList};
    }
}

public type ProductRequest record {|
    string product_id = "";
|};

public type CreateProductRequest record {|
    string name = "";
    string description = "";
    float price = 0.0;
|};

public type ProductResponse record {|
    string product_id = "";
    string name = "";
    string description = "";
    float price = 0.0;
    string created_at = "";
|};

public type ProductListResponse record {|
    ProductResponse[] products = [];
|};

public type Empty record {|
|};

const string PRODUCT_DESC = "0A0D70726F647563742E70726F746F120E70726F6475637473657276696365221A0A0E50726F6475637452657175657374120812070A70726F647563745F6964180122550A1443726561746550726F6475637452657175657374120A120A6E616D6518011214120B6465736372697074696F6E1802120D120570726963651803220009120570726963652289010A0F50726F64756374526573706F6E7365120C120A70726F647563745F696418011210120A6E616D6518021214120B6465736372697074696F6E1803120D120570726963651804220009120570726963651212120A63726561746564X61741805220009120A637265617465644174223F0A1350726F647563744C697374526573706F6E7365122812080870726F647563747318011A100A0F50726F64756374526573706F6E736512020801220009120870726F647563747322070A05456D70747932D5010A0E50726F6475637453657276696365124C120A47657450726F64756374121550726F6475637452657175657374X1A0F50726F64756374526573706F6E7365220009125A120D43726561746550726F6475637412144372656174650A50726F6475637452657175657374X1A0F50726F64756374526573706F6E7365220009124F120C4C69737450726F6475637473120A456D7074791A1350726F647563744C697374526573706F6E7365220009620670726F746F33";
