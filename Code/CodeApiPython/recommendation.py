# recommendation.py
from models import UserData

def generate_recommendations(user_data: UserData):
    # À compléter avec un vrai algorithme de machine learning

    # Pour l'instant, retourne une liste de jeux en exemple
    recommendations = [
        {"game_id": 101, "game_name": "Pandemic"},
        {"game_id": 102, "game_name": "Catan"},
        {"game_id": 103, "game_name": "Ticket to Ride"}
    ]
    return recommendations
