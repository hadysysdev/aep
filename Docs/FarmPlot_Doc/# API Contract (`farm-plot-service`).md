# API Contract (`farm-plot-service`)

**Base Path:** `/v1`

## Resource: `/farms`
*   `POST /farms`: Register a new farm.
    *   **Request Body:** `CreateFarmRequest`
    *   **Response:** `201 Created`, `FarmResponse`
*   `GET /farms/{farmIdentifier}`: Retrieve a specific farm.
    *   **Response:** `200 OK`, `FarmResponse`
*   `GET /farms`: List all farms (paginated).
    *   **Response:** `200 OK`, `Page<FarmResponse>`
*   `PUT /farms/{farmIdentifier}`: Update an existing farm.
    *   **Request Body:** `UpdateFarmRequest`
    *   **Response:** `200 OK`, `FarmResponse`
*   `DELETE /farms/{farmIdentifier}`: Delete a farm.
    *   **Response:** `204 No Content`

## Resource: `/plots`
*   `POST /plots`: Define a new plot.
    *   **Request Body:** `CreatePlotRequest`
    *   **Response:** `201 Created`, `PlotResponse`
*   `GET /plots/{plotIdentifier}`: Retrieve a specific plot.
    *   **Response:** `200 OK`, `PlotResponse`
*   `GET /plots`: List all plots (paginated), can be filtered by `farmIdentifier`.
    *   **Response:** `200 OK`, `Page<PlotResponse>`
*   `PUT /plots/{plotIdentifier}`: Update an existing plot.
    *   **Request Body:** `UpdatePlotRequest`
    *   **Response:** `200 OK`, `PlotResponse`
*   `DELETE /plots/{plotIdentifier}`: Delete a plot.
    *   **Response:** `204 No Content`

## Sub-Resource: `/plots/{plotIdentifier}/land-tenure`
*   `GET /plots/{plotIdentifier}/land-tenure`: Get land tenure details for a plot.
    *   **Response:** `200 OK`, `LandTenureResponse`
*   `PUT /plots/{plotIdentifier}/land-tenure`: Create or update land tenure details.
    *   **Request Body:** `CreateOrUpdateLandTenureRequest`
    *   **Response:** `200 OK`, `LandTenureResponse`
*   `DELETE /plots/{plotIdentifier}/land-tenure`: Delete land tenure details.
    *   **Response:** `204 No Content`

## Sub-Resource: `/farms/{farmIdentifier}/pois` & `/plots/{plotIdentifier}/pois`
*   `POST .../pois`: Create a Point of Interest for a farm or plot.
    *   **Request Body:** `CreatePointOfInterestRequest`
    *   **Response:** `201 Created`, `PointOfInterestResponse`
*   `GET .../pois`: List all POIs for a farm or plot.
    *   **Response:** `200 OK`, `List<PointOfInterestResponse>`